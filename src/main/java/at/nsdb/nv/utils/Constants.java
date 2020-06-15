package at.nsdb.nv.utils;

/**
 * common application constants
 */
public final class Constants {

	/*-----------------------------------------------------------------------------
	/*
	/* fieldnames of the database
	/* 
	/*-----------------------------------------------------------------------------
	 */
	/** predefined node-labels */
	public static enum labelName {
		Person
	}

	/** variable node-labels for stati of :Person */
	public static enum labelNameVar {
		Healthy, InIncubation, Ill, Immune
	}

	/** predefined relation-types */ 
	public static enum relType {
		CanInfect
	}

	/** variable relation-types */ 
	public static enum relTypeVar {
		HasInfected
	}

	// attributes of relations
	public static enum relAttribute {
		distance, day
	}

	// attributes of nodes
	public static enum fieldName {
		id, name, longitude, latitude, dayOfInfection, incubationPeriod, illnessPeriod
	}
    
}