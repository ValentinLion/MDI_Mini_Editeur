package commands;

import controleur.Buffer;

public class Copier implements Command{
	private Buffer buffer;

	public Copier(Buffer buffer) {
		this.buffer=buffer;
	}
	@Override
	public void exec() {
		buffer.copier();
	}

	public void setBuffer(Buffer buffer) {
		this.buffer=buffer;
	}

}