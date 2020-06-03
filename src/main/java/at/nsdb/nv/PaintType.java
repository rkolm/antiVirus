package at.nsdb.nv;

public class PaintType {
	
	private int xPos, yPos;
	private Person.status status;
	
	public PaintType(int xPos, int yPos, at.nsdb.nv.Person.status status) {
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
