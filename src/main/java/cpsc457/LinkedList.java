package cpsc457;

import cpsc457.doNOTmodify.Pair;
import cpsc457.Node;

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
		newNode.contents = (Comparable)t;
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
    public static class MergeSort<T> {
	
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
			
			Node sortedHead = msort(list.head);	

			list.head = sortedHead;
		}
		
		public Node msort(Node head)
		{	
			// Split the list
			Pair<Node,Node> pair = split(head);
			
			Node head1;
			Node head2;
			if (pair.fst().next != null)
				head1 = msort(pair.fst());
			else
				head1 = pair.fst();
			
			if (pair.snd().next != null)
				head2 = msort(pair.snd());
			else
				head2 = pair.snd();
			
			// Time to merge!
			return merge(head1, head2);			
		}
		
		// merges two linked lists and returns the merged list head
		public Node merge(Node head1, Node head2){
			Node headPointer;
			Node walkPointer;
			// Check some specifit conditions
			if (head1 == null && head2 == null)
				return null;
			if (head1 == null)
				return head2;
			if (head2 == null)
				return head1;

			// Setup the pointers
			Node leftPointer = head1.next;
			Node rightPointer = head2.next;
			if (head1.contents.compareTo(head2.contents) < 0)
			{
				headPointer = head1;
				headPointer.next = head2;
				walkPointer = head2;
			}
			else
			{
				headPointer = head2;
				headPointer.next = head1;
				walkPointer = head1;
			}

			while (leftPointer != null || rightPointer != null)
			{
				// Both pointers have a value
				// Assuming here DESCENDING list from head.
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
			
			
		// Helper Functions	
		public Pair<Node,Node> split(Node node)
		{
			Node a = node;
			Node walker1 = node;
			Node walker2 = node;
			
			// Just covering the bases if the list happens to be empty
			if (node == null)
				return new Pair(null,null);
			
			while (walker2.next != null && walker2.next.next != null)
			{
				walker1 = walker1.next;
				walker2 = walker2.next.next;
			}	
			
			Node b = walker1.next;
			walker1.next = null;
			
			return new Pair(a,b);
			
			// Alternate implementation - UNTESTED
/* 			// 1. Find the center of the LL
			//int index = list.size/2;
			Node a = new Node();
			Node b = new Node();
			Node temp = node;
			
			// Find size of list
			int i = 0;
			a = node;
			for(; temp.next != null; i++)
			{
				temp = temp.next;
			}
			
			// Find middle of list
			i = i/2;
			temp = node;
			for(int j = 0; j < i; j++)
			{
				node = node.next;
				temp = temp.next;
			}
			
			node.next = null;
			b = temp.next;
			return new Pair(a,b); */
		}
	}
}
