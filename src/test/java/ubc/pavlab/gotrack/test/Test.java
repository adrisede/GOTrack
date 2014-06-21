package ubc.pavlab.gotrack.test;

import java.util.Scanner;

public class Test {
	/**
	 * For input only
	 */
	static Scanner scanner = new Scanner(System.in);

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Object username = null;
		username = askQuestion("What is your name?: ");
		System.out.println("Hello " + username + ". Welcome!");

	}

	private static Object askQuestion(String question) {
		System.out.print(question);
		return scanner.next();
	}

}
