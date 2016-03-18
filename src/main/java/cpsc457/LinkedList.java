package cpsc457;

import cpsc457.doNOTmodify.Pair;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedList<T> implements Iterable<T> {
 
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
		//Head
		private Node head;
		//Tail
		private Node tail;
		//Size (not required)
		private int size;
		//Critical Section
 
	//Constructor
    public LinkedList() {
		//Set head and tail to null
		head = null;
		tail = null;
		//Set size to zero
		size = 0;
		//Create new instance for the critical section
    }

	//Returns the size of the list
    public int size() {
        return size; //either iterate through all the list and count
					//or create an attribute that stores the size and changes
					//every time we add or remove a node
    }
	
	//Checks if the list is empty
	public boolean isEmpty() {
		if (size == 0)
			return true;
		else
			return false;
    }
	
	//Deletes all the nodes in the list
	public void clear() {
		//just set the head and tail to null (the garbage collector takes care of the rest)
		head = null;
		tail = null;
			//cpp developers: be careful, you have to destroy them first
		
		//What if the merge sort is running now in a thread
			//I should not be able to delete the nodes (and vice versa)
			//Thus run this and everything else in a critical section
    }
	
	//Adds a new node to the list at the end (tail)
    public LinkedList<T> append(T t) {
		Node newNode = new Node();
		newNode.setContents(t);
		//Check if it is empty 
		if (size == 0)
			head = tail = newNode;
		//Else add to the tail and move the tail to the end
		else
		{
			//tail.next = t    then		tail = t
			tail.setNext(newNode);
			tail = newNode;
		}	
		
		//Do not forget to increment the size by 1 (if you have it as an attribute)
		size++;
		
		return this;
    }

	//Gets a node's value at a specific index
    public T get(int index) {
		//Iterate through the list
			//Create a new pointer that starts at the head
		Node pointer = new Node();
		pointer = head;
			//Keeps moving forward (pt = pt.next) for index times
		for(int i = 0; i <= index; i++)
		{
			pointer = pointer.getNext();
		//Make sure not to exceed the size of the list (else return null)
			if(pointer.getContents() == null)
				return null;
		}
		
		return (T)pointer.getContents();
    }
	
	@Override
    public Iterator<T> iterator() {
		return null;
    }
	
	//The next two functions, are being called by the static functions at the top of this page
	//These functions are just wrappers to prevent the static function from deciding which
	//sorting algorithm should it use.
	//This function will decide which sorting algorithm it should use 
	//(we only have merge sort in this assignment)
	
	//Sorts the link list in serial
    private void sort(Comparator<T> comp) {
	
		new MergeSort<T>(comp).sort(this); //Run this within the critical section (as discussed before)
		
		//It might not allow you to use this inside critical
			//Create a final pointer = this then use that pointer
    }

	//Sorts the link list in parallel (using multiple threads)
    private void par_sort(Comparator<T> comp) {
		new MergeSort<T>(comp).parallel_sort(this); //Run this within the critical section (as discussed before)
    }

	//Merge sort
    public class MergeSort<T> {
	
		//Variables (attributes)
			//ExecutorService
			//Depth limit
	
		//Comparison function
		final Comparator<T> comp;

		//Constructor
		public MergeSort(Comparator<T> comp) {
			this.comp = comp;
		}

		//#####################
		//# Sorting functions #
		//#####################
		//The next two functions will simply call the correct function 
		//to merge sort the link list and then they will fix its 
		//attributes (head and tail pointers)
		
		public void sort(LinkedList<T> list) {
			// Check if there is only one node
			if (list.size <= 1)
				return;			
			
			list = msort(list);	
		}
		
		public LinkedList msort(LinkedList list)
		{	
			// Split the list
			Pair<LinkedList,LinkedList> pair = split(list);
			
			LinkedList list1;
			LinkedList list2;
			if (pair.fst().head.next != null)
				list1 = msort(pair.fst());
			else
				list1 = pair.fst();
			
			if (pair.snd().head.next != null)
				list2 = msort(pair.snd());
			else
				list2 = pair.snd();
			
			// Time to merge!
			return merge(list1, list2);			
		}
		
		// merges two linked lists and returns the merged list head
		public LinkedList merge(LinkedList list1, LinkedList list2){
			LinkedList<Node> LL = new LinkedList();
			Node head1 = list1.head;
			Node head2 = list2.head;
			
			while (head1.next != null && head2.next != null)
			{
				// one list is empty
				if (head1 == null)
				{
					LL.append(head2);
					head2 = head2.next;
					continue;
				}
				if (head2 == null)
				{
					LL.append(head1);
					head1 = head1.next;
					continue;
				}
				
				// Both heads have a value
				// Assuming here DESCENDING list from head.
				if (head1.contents.compareTo(head2.contents))
				{
					LL.append(head1);
					head1 = head1.next;
				}
				else
				{
					LL.append(head2);
					head2 = head2.next;
				}
			}
			
			return LL;
		}

		public void parallel_sort(LinkedList<T> list) {			
		}
		
		//#########
		//# Steps #
		//#########
		
		//The main merge sort function (parrallel_msort and msort)
			//Split the list to two parts
			//Merge sort each part
			//Merge the two sorted parts together
		
		//Splitting function
			//Run two pointers and find the middle of the a specific list
			//Create two new lists (and break the link between them)
			//It should return pair (the two new lists)
		
		//Merging function
			//1- Keep comparing the head of the two link lists
			//2- Move the smallest node to the new merged link list
			//3- Move the head on the list that lost this node
			
			//4- Once one of the two lists is done, append the rest of the 
			//	 second list to the tail of the new merged link list
	}
	
	
	// nodes used in linked list
	public class Node<T extends Comparable<T>> {
		public T contents;
		public Node next;
		
		public Node()
		{
			contents = null;
			next = null;
		}
		
		public Node(T t)
		{
			contents = t;
			next = null;
		}
		
		public T getContents()
		{
			return contents;
		}
		
		public void setContents(T t)
		{
			contents = t;
		}
		
		public Node getNext()
		{
			return next;
		}
		
		public void setNext(Node n)
		{
			next = n;
		}
	}

	// Helper Functions	
	public Pair<LinkedList,LinkedList> split(LinkedList list)
	{
		// TODO
		// 1. Find the center of the LL
		int index = list.size/2;
		LinkedList a = new LinkedList();
		LinkedList b = new LinkedList();
		
		// 2. Set the "left of center" node.next = null
		Node temp = list.head;
		for (int i = 0; i < index; i++)
		{
			a.append(temp.getContents());
			temp = temp.next;
		}
		
		for (int i = index; i < list.size; i++)
		{
			b.append(temp.getContents());
			temp = temp.next;
		}
		
		return new Pair(a,b);
	}
}
