package com.babai.wml;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.lsp4j.launch.LSPLauncher.createServerLauncher;

public class Main {

	public static void main(String[] args) throws Exception {
		var server = new WMLLanguageServer();
		
		// Initialize a simple JSON-RPC connection over stdin/stdout
		Launcher<LanguageClient> launcher = createServerLauncher(server, System.in, System.out);
		LanguageClient client = launcher.getRemoteProxy();
		
		server.connect(client);
		launcher.startListening();
	}

	public static class WMLLanguageServer implements LanguageServer, LanguageClientAware {

		private LanguageClient client;

		public void connect(LanguageClient client) {
			this.client = client;
		}

		@Override
		public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
			ServerCapabilities capabilities = new ServerCapabilities();
			capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

			InitializeResult result = new InitializeResult(capabilities);
			return CompletableFuture.completedFuture(result);
		}

		@Override
		public CompletableFuture<Object> shutdown() {
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void exit() {
			System.exit(0);
		}

		@Override
		public TextDocumentService getTextDocumentService() {
			return new TextDocumentService() {
				@Override
				public void didOpen(DidOpenTextDocumentParams params) {
					System.out.println("File opened: " + params.getTextDocument().getUri());
				}

				@Override
				public void didChange(DidChangeTextDocumentParams params) {}

				@Override
				public void didClose(DidCloseTextDocumentParams params) {}

				@Override
				public void didSave(DidSaveTextDocumentParams params) {}
			};
		}

		@Override
		public WorkspaceService getWorkspaceService() {
			return new WorkspaceService() {
				@Override
				public void didChangeConfiguration(DidChangeConfigurationParams params) {}

				@Override
				public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {}
			};
		}
	}
}
