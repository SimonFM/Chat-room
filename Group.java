import java.util.ArrayList;

public class Group {
	private ArrayList<String> members;
	
	/**
	 * Constructor for the group, initializes the arraylist
	 */
	Group(){
		members = new ArrayList<String>();
	}
	/**
	 * Adds a member to the group.
	 * @param name
	 */
	void addMember(String name){
		if(!members.contains(name)){
			members.add(name);
			System.out.println(name+" was added");
		}else{
			System.out.println("Member is already in the group");
		}
	}
	/**
	 * Returns true or false if a member is in a group.
	 * @param name
	 * @return
	 */
	boolean lookup(String name){
		for(String c : members) if(c.equals(name)) return true;
		return false;
	}
	
	/**
	 * Removes a member from the group.
	 * @param name
	 */
	void removeMember(String name){
		if(members.contains(name)){
			members.remove(name);
			System.out.println(name+" was removed");
		}else System.out.println(name+" member is not in the group");
	}
	
}
