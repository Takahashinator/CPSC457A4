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