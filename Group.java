import java.util.ArrayList;


public class Group {
	private ArrayList<String> members;
	
	Group(){
		members = new ArrayList<String>();
	}
	void addMember(String name){
		if(!members.contains(name)){
			members.add(name);
			System.out.println(name+" was added");
		}
		else{
			System.out.println("Member is already in the group");
		}
	}
	
	boolean lookup(String name){
		for(String c : members) if(c.equals(name)) return true;
		return false;
	}
	
	void removeMember(String name){
		if(members.contains(name)){
			members.remove(name);
			System.out.println(name+" was removed");
		}
		else System.out.println("Member is not in the group");
	}
	
}
