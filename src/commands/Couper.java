package commands;

import modele.Buffer;

public class Couper implements Command {

	private Buffer buffer;

	public Couper(Buffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public void exec() {
		buffer.couper();
	}

}
