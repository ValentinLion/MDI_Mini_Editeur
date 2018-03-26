package commands;

import controleur.Buffer;

public class Coller implements Command{
	private Buffer buffer;

	public Coller(Buffer buffer) {
		this.buffer=buffer;
	}
	
	@Override
	public void exec() {
		buffer.coller();
	}

	public void setBuffer(Buffer buffer) {
		this.buffer=buffer;
	}

}