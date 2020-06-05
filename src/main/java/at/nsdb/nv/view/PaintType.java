package at.nsdb.nv.view;

import at.nsdb.nv.model.Person;

public class PaintType {
	
	private int xPos, yPos;
	private Person.status status;
	
	public PaintType(int xPos, int yPos, Person.status status) {
		super();
		this.xPos = xPos;
		this.yPos = yPos;
		this.status = status;
	}

	public Person.status getStatus() {
		return status;
	}

	public void setStatus( Person.status status) {
		this.status = status;
	}

	public int getXPos() {
		return xPos;
	}

	public int getYPos() {
		return yPos;
	}
}
