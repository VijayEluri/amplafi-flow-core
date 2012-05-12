package org.amplafi.flow;

import static org.amplafi.flow.FlowConstants.FSREDIRECT_URL;

import java.io.Writer;
import java.net.URI;

import org.amplafi.flow.web.FlowResponse;

public class BaseFlowResponse implements FlowResponse {

	private final Writer writer;
	private Exception exception;
	private String errorMessage;
	private FlowState flowState;
	private URI redirectUri;

	public BaseFlowResponse(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void setError(String message, Exception exception) {
		errorMessage = message;
		this.exception = exception;
	}

	@Override
	public void render(FlowRenderer renderer) {
		renderer.render(this);
	}

	@Override
	public boolean hasErrors() {
		return exception != null || errorMessage != null;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public FlowState getFlowState() {
		return flowState;
	}

	@Override
	public Exception getException() {
		return exception;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public void setFlowState(FlowState flowState) {
		this.flowState = flowState;
	}

	@Override
	public URI getRedirect() {
		if (redirectUri == null && flowState != null 
				&& flowState.isCompleted() && flowState.isPropertySet(FSREDIRECT_URL)) {
			//If redirect URI wasn't explicitly set, see if the end flow redirect was planned.
			return flowState.getProperty(FSREDIRECT_URL, URI.class);
		}
		return redirectUri;
	}

	@Override
	public boolean isRedirectSet() {
		return getRedirect() != null;
	}

	@Override
	public void setRedirectURI(URI redirectUri) {
		this.redirectUri = redirectUri;
	}
}