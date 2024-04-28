package com.github.nebelnidas.kfdl.cli.provider.builtin;

import com.beust.jcommander.Parameters;

import com.github.nebelnidas.kfdl.cli.KfdlCli;
import com.github.nebelnidas.kfdl.cli.provider.CliCommandProvider;
import com.github.nebelnidas.kfdl.core.Kfdl;

/**
 * Provides the default {@code automatch} command.
 */
public class RunCliCommandProvider implements CliCommandProvider {
	private static final String commandName = "run";
	private final RunCommand command = new RunCommand();

	@Parameters(commandNames = {commandName})
	class RunCommand {
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

	@Override
	public Object getDataHolder() {
		return command;
	}

	@Override
	public void processArgs() {
		Kfdl downloader = new Kfdl();
		downloader.run();

		KfdlCli.LOGGER.info("Done!");
	}
}
