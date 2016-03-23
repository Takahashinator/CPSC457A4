package cpsc457;

public class Node<T extends Comparable<T>> {
	public T contents;
	public Node<T> next;
	
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