package cpsc457;

import cpsc457.doNOTmodify.Pair;
import cpsc457.Node;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedList<T extends Comparable<T>> implements Iterable<T> {
 
	//####################
	//# Static Functions #
	//####################
	
	//We do not want the testers to have the freedom of specifying the comparison function
	//Thus, we will create wrappers for them that they can use and inside these wrappers
	//we will have the comparison function predefined
		//These two static wrappers, will simply call the sort method in the list passed as parameter,
		//and they pass the comparison function as well
	
	public static <T extends Comparable<T>> void par_sort(LinkedList<T> list) {
		list.par_sort(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        });
    }

    public static <T extends Comparable<T>> void sort(LinkedList<T> list){
        list.sort(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        });
    }
 
	//############
	//# LinkList #
	//############
	
	//Variables (attributes)
	private Node head;
	private Node tail;
	private int size;
		
	// Linked List Concurrency Control
	// Only one thread will be able to alter the linked list at any time
	private static Semaphore LLAlterationLock = new Semaphore(1);
 
	//Constructor
    public LinkedList() 
	{
		//Set head and tail to null
		head = null;
		tail = null;
		//Set size to zero
		size = 0;
    }
	
	public <T extends Comparable<T>> void printContents()
	{
		acquireLLLock();
		Node<T> ptr = head;
		while (ptr != null)
		{
			System.console().writer().print("[");
			System.console().writer().print(ptr.contents);
			System.console().writer().print("]-");
			ptr = ptr.next;
		}
		
		System.console().writer().println("");
		releaseLLLock();
	}

	//Returns the size of the list
    public int size() 
	{
        return size; //either iterate through all the list and count
					//or create an attribute that stores the size and changes
					//every time we add or remove a node
    }
	
	//Checks if the list is empty
	public boolean isEmpty() 
	{
		if (size == 0)
			return true;
		else
			return false;
    }
	
	//Deletes all the nodes in the list
	public void clear() 
	{
		acquireLLLock();
		//just set the head and tail to null (the garbage collector takes care of the rest)
		head = null;
		tail = null;
		releaseLLLock();
    }
	
	//Adds a new node to the list at the end (tail)
    public LinkedList<T> append(T t) 
	{
		if (t == null)
			return this;
		
		acquireLLLock();
		Node<T> newNode = new Node();
		newNode.contents = t;
		//Check if it is empty 
		if (size == 0)
			head = tail = newNode;
		//Else add to the tail and move the tail to the end
		else
		{
			//tail.next = t    then		tail = t
			tail.next = newNode;
			tail = newNode;
		}
		
		//Do not forget to increment the size by 1 (if you have it as an attribute)
		size++;
		releaseLLLock();
		return this;
    }

	// Gets a node's value at a specific index
	// Note - this method is not used in our implementation
    public <T extends Comparable<T>> T get(int index) 
	{
		acquireLLLock();
		//Create a new pointer that starts at the head
		Node<T> pointer = head;
		//Keeps moving forward (pt = pt.next) for index times
		for(int i = 0; i <= index; i++)
		{
			pointer = pointer.next;
		//Make sure not to exceed the size of the list (else return null)
			if(pointer.contents == null)
			{
				// Make sure to release in this case
				LLAlterationLock.release();
				return null;
			}
		}
		
		releaseLLLock();		
		return pointer.contents;
    }
	
	private void acquireLLLock()
	{
		try
		{
			LLAlterationLock.acquire();
		} catch (InterruptedException e) 
		{
			// Uncertain what to do in this case...
			e.printStackTrace();
		}
	}
	
	private void releaseLLLock()
	{
		LLAlterationLock.release();
	}
	
	
	@Override
    public Iterator<T> iterator() 
	{
		Iterator<T> it = new Iterator<T>() 
		{
            private Node<T> ptr = head;

            @Override
            public boolean hasNext() 
			{
                return (ptr != null && ptr.next != null);
            }

            @Override
            public T next() 
			{
				if (ptr != null)
				{
					T val = (T)ptr.contents;
					if (hasNext())
						ptr=ptr.next;	
					return val;					
				}
				return null;
            }

            @Override
            public void remove() 
			{
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }
	
	//The next two functions, are being called by the static functions at the top of this page
	//These functions are just wrappers to prevent the static function from deciding which
	//sorting algorithm should it use.
	//This function will decide which sorting algorithm it should use 
	//(we only have merge sort in this assignment)
	
	//Sorts the link list in serial
    private void sort(Comparator<T> comp) 
	{
	
		new MergeSort<T>(comp).sort(this); //Run this within the critical section (as discussed before)
		
		//It might not allow you to use this inside critical
			//Create a final pointer = this then use that pointer
    }

	//Sorts the link list in parallel (using multiple threads)
    private void par_sort(Comparator<T> comp) 
	{
		new MergeSort<T>(comp).parallel_sort(this); //Run this within the critical section (as discussed before)
    }

	//Merge sort
    public static class MergeSort<T extends Comparable> 
	{
	
		//Variables (attributes)			
		private static int maxThreads = 10;
		private static ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
		private static int numThreadsUsed = 0; // Must be protected in critical section!
		private static Semaphore threadAccessLock = new Semaphore(1);
	
		//Comparison function
		final Comparator<T> comp;
		
		//Constructor
		public MergeSort(Comparator<T> comp) 
		{
			this.comp = comp;
		}
		
		// The entry level method for the serial msort
		// A generic linked list is provided and the head is set to the newly 
		// sorted linked list
		public <T extends Comparable<T>> void sort(LinkedList<T> list) 
		{
			// Check if there is only one node
			if (list.size <= 1)
				return;	
			
			Node<T> sortedHead = msort(list.head);	

			list.head = sortedHead;
		}
		
		// Recursive method for sorting a list
		// The head of the linked list is provided and
		// the head of the sorted list is returned
		public <T extends Comparable<T>> Node msort(Node head)
		{	
			// Split the list
			Pair<Node<T>,Node<T>> pair = split(head);
			
			Node<T> head1 = null;
			Node<T> head2 = null;
			if (pair.fst() != null)
				if (pair.fst().next != null)
					head1 = msort(pair.fst());
				else
					head1 = pair.fst();
					
			if (pair.snd() != null)
				if (pair.snd().next != null)
					head2 = msort(pair.snd());
				else
					head2 = pair.snd();
			// Time to merge!
			return merge(head1, head2);			
		}

		// The entry level method for the paralell msort
		// A generic linked list is provided and the head is set to the newly 
		// sorted linked list
		public <T extends Comparable<T>> void parallel_sort(LinkedList<T> list) 
		{
			if (list.size <= 1)
				return;	
			
			Callable th = new parMsortThread(list.head);
			Future future = executor.submit(th);

			try
			{
				list.head = (Node<T>)future.get();
			} catch (InterruptedException e) 
			{
				list.head = null;
				e.printStackTrace();
			} catch (ExecutionException e)
			{
				e.printStackTrace();
			}
		}		
		
		// This is the main worker thread for the paralell msort
		// The call method returns the head node of the sorted list and works recursively
		// A counter is used to keep track of the number of threads currently in use
		// If all the threads are used up, the current thread will perform the rest
		// of the calculations in serial
		private class parMsortThread<T extends Comparable<T>> implements Callable 
		{

			Node<T> head;
			
			parMsortThread(Node<T> head) 
			{
				this.head = head;
			}
			
			@Override
			public Node<T> call() 
			{
					// Every time a new thread is run we increment the threads used counter
					safeIncrementThreadUse();
					Pair<Node<T>,Node<T>> pair = split(head);
					Node<T> head1 = null;
					Node<T> head2 = null;
					Future<Node<T>> future1 = null;
					Future<Node<T>> future2 = null;
					
					if (pair.fst() != null)
						if (numThreadsUsed < maxThreads && pair.fst().next != null)
						{
							// create new thread
							Callable th1 = new parMsortThread(pair.fst());
							future1 = executor.submit(th1);
						}
						else	
							head1 = msort(pair.fst());
							
					
					if (pair.snd() != null)
						if (numThreadsUsed < maxThreads && pair.snd().next != null)
						{
							// create new thread
							Callable th2 = new parMsortThread(pair.snd());
							future2 = executor.submit(th2);
						}
						else				
							head2 = msort(pair.snd());
											
					if (future1 != null)
					{
						try
						{
							head1 = (Node<T>)future1.get();
						} catch (InterruptedException e) 
						{
							e.printStackTrace();
						} catch (ExecutionException e)
						{
							e.printStackTrace();
						}
					}
					if (future2 != null)
					{
						try
						{
							head2 = (Node<T>)future2.get();
						} catch (InterruptedException e) 
						{
							e.printStackTrace();
						} catch (ExecutionException e)
						{
							e.printStackTrace();
						}
					}
					// merge... but dont attempt to merge until BOTH results are available	
					Node<T> result = merge(head1, head2);
					safeDecrementThreadUse();		
					return result;
			}
		}
		
		//----------------------//
		// Concurrency Functions//	
		//----------------------//
		
		private void safeIncrementThreadUse()
		{
			try
			{
				threadAccessLock.acquire();
				numThreadsUsed++;
			} catch (InterruptedException e) 
			{
				// Uncertain what to do in this case...
				e.printStackTrace();
			} finally
			{
				// Should always release just in case
				threadAccessLock.release();
			}
		}
		
		private void safeDecrementThreadUse()
		{
			try
			{
				threadAccessLock.acquire();
				numThreadsUsed--;
			} 
			catch (InterruptedException e) 
			{
				// Uncertain what to do in this case...
				e.printStackTrace();
			} finally
			{
				// Should always release just in case
				threadAccessLock.release();
			}
		}
		
		//-----------------//
		// Helper Functions//	
		//-----------------//
		public <T extends Comparable<T>> Pair<Node<T>,Node<T>> split(Node<T> node)
		{
			Node<T> a = node;
			Node<T> walker1 = node;
			Node<T> walker2 = node;
			
			// Just covering the bases if the list happens to be empty
			if (node == null)
				return new Pair(null,null);
			
			while (walker2.next != null && walker2.next.next != null)
			{
				walker1 = walker1.next;
				walker2 = walker2.next.next;
			}	
			
			Node<T> b = walker1.next;
			walker1.next = null;
			
			return new Pair(a,b);
		}
		
		// merges two linked lists and returns the merged list head
		public <T extends Comparable<T>> Node<T> merge(Node<T> head1, Node<T> head2)
		{
			Node<T> headPointer; // pointer to keep track of the head node so return can be easy
			Node<T> walkPointer;
			// Check some specific conditions
			if (head1 == null && head2 == null)
				return null;
			if (head1 == null)
				return head2;
			if (head2 == null)
				return head1;

			// Setup the pointers
			Node<T> leftPointer = head1;
			Node<T> rightPointer = head2;
			if (head1.contents.compareTo(head2.contents) < 0)
			{
				headPointer = head1;
				walkPointer = head1;
				leftPointer = leftPointer.next;
			}
			else
			{
				headPointer = head2;
				walkPointer = head2;
				rightPointer = rightPointer.next;
			}

			while (leftPointer != null && rightPointer != null)
			{
				// Both pointers have a value
				// Assuming here ASCENDING order from head.
				if (leftPointer.contents.compareTo(rightPointer.contents) < 0)
				{
					walkPointer.next = leftPointer;
					leftPointer = leftPointer.next;
				}
				else
				{
					walkPointer.next = rightPointer;
					rightPointer = rightPointer.next;
				}
				// walkPointer walks...
				walkPointer = walkPointer.next;
			}

			// If there are still nodes on the right but not on the left
			if (leftPointer == null && rightPointer != null)
			{
				walkPointer.next = rightPointer;
			}
			// If there are still nodes on the left but not on the right
			else if (rightPointer == null && leftPointer != null)
			{
				walkPointer.next = leftPointer;
			}

			return headPointer;
		}
	}
}
