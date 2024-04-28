package com.github.nebelnidas.kfdl.cli;

import com.github.nebelnidas.kfdl.cli.provider.builtin.RunCliCommandProvider;

public class Main {
	public static void main(String[] args) {
		// Instantiate the CLI handler. We don't accept unknown parameters,
		// since this is the base implementation where only known
		// providers are registered.
		KfdlCli kfdlCli = new KfdlCli(false);

		// Register all default providers.
		kfdlCli.registerCommandProvider(new RunCliCommandProvider());

		// Parse, handle errors, delegate to the correct provider.
		kfdlCli.processArgs(args);
	}
}
