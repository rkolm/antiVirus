package at.nsdb.nv.model;

import at.nsdb.nv.Config;

public class StatisticADay {
	// overall persons
	int numbPersonsHealthy, numbPersonsInIncubation, numbPersonsIll, numbPersonsImmune;
	int newNumbPersonsInIncubation, newNumbPersonsIll, newNumbPersonsImmune;

	public int getNumbPersonsHealthy() {
		return numbPersonsHealthy;
	}
	public void setNumbPersonsHealthy(int numbPersonsHealthy) {
		this.numbPersonsHealthy = numbPersonsHealthy;
	}
	public int getNumbPersonsInIncubation() {
		return numbPersonsInIncubation;
	}
	public void setNumbPersonsInIncubation(int numbPersonsInIncubation) {
		this.numbPersonsInIncubation = numbPersonsInIncubation;
	}
	public int getNumbPersonsIll() {
		return numbPersonsIll;
	}
	public void setNumbPersonsIll(int numbPersonsIll) {
		this.numbPersonsIll = numbPersonsIll;
	}
	public int getNumbPersonsImmune() {
		return numbPersonsImmune;
	}
	public void setNumbPersonsImmune(int numbPersonsImmune) {
		this.numbPersonsImmune = numbPersonsImmune;
	}
	public int getNewNumbPersonsInIncubation() {
		return newNumbPersonsInIncubation;
	}
	public void setNewNumbPersonsInIncubation(int newNumbPersonsInIncubation) {
		this.newNumbPersonsInIncubation = newNumbPersonsInIncubation;
	}
	public int getNewNumbPersonsIll() {
		return newNumbPersonsIll;
	}
	public void setNewNumbPersonsIll(int newNumbPersonsIll) {
		this.newNumbPersonsIll = newNumbPersonsIll;
	}
	public int getNewNumbPersonsImmune() {
		return newNumbPersonsImmune;
	}
	public void setNewNumbPersonsImmune(int newNumbPersonsImmune) {
		this.newNumbPersonsImmune = newNumbPersonsImmune;
	}
	
	public double getR() {
		if( numbPersonsInIncubation == 0) return 0;
		else return (double) newNumbPersonsInIncubation / numbPersonsInIncubation *
			Config.getCanInfectGaussValue( "Avg");
	}
	
	public double getQ() {
		return (double) (newNumbPersonsInIncubation + numbPersonsInIncubation) /
			(numbPersonsHealthy + numbPersonsInIncubation + numbPersonsIll + numbPersonsImmune);
	}	
}
