package cpsc457;

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
}