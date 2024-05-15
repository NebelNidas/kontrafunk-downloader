package com.github.nebelnidas.kfdl.cli.provider.builtin;

import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.SneakyThrows;

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
		@Parameter(names = {BuiltinCliParameters.WORKING_DIRECTORY}, required = true)
		Path workingDirectory;

		@Parameter(names = {BuiltinCliParameters.SAVE_FILE})
		Path saveFile;

		@Parameter(names = {BuiltinCliParameters.MAX_PARALLEL_DOWNLOADS})
		int maxParallelDownloads = 2;
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
	@SneakyThrows
	public void processArgs() {
		if (command.saveFile == null) {
			command.saveFile = command.workingDirectory.resolve("kfdl-state.txt");
		}

		if (!Files.exists(command.workingDirectory)) {
			Files.createDirectories(command.workingDirectory);
		}

		Kfdl downloader = new Kfdl(command.workingDirectory, command.saveFile, command.maxParallelDownloads);
		downloader.run();

		KfdlCli.LOGGER.info("Done!");
	}
}
