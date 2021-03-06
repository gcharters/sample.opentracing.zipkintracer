/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing.zipkin;

import java.util.concurrent.TimeUnit;

import com.ibm.ws.opentracing.zipkin.OpentracingZipkinTracerFactory.Config;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Reporter;
import zipkin.reporter.Sender;
import zipkin.reporter.okhttp3.OkHttpSender;

/**
 * This class wraps the Brave tracer to provide a
 * generic tracer implementation to the opentracing feature.
 *
 * In theory the Brave tracer could be replaced by another tracer
 * implementation.
 *
 */
public class OpentracingZipkinTracer implements Tracer {
	Tracer tracer;

	/**
	 * Creates and returns a tracer that is uses 
	 * a serviceName and the location of a zipkin host/port
	 * to provide tracing capability.
	 *
	 * @param serviceName
	 * @param config
	 */
	public OpentracingZipkinTracer(String serviceName, Config config) {
		String traceServiceUrl = "http://"+config.host()+":"+config.port()+"/api/v1/spans";
		OkHttpSender.Builder senderBuilder = OkHttpSender.builder().endpoint(traceServiceUrl).compressionEnabled(config.compress());
		if (config.maxRequests() != Integer.MIN_VALUE) {
			senderBuilder.maxRequests(config.maxRequests());
		}
		if (config.encoding() != null) {
			senderBuilder.encoding(config.encoding());
		}
		if (config.maxMessageSize() != Integer.MIN_VALUE) {
			senderBuilder.messageMaxBytes(config.maxMessageSize());
		}
		Sender sender = senderBuilder.build();
		AsyncReporter.Builder reporterBuilder = AsyncReporter.builder(sender);
		if (config.closeTimeout() != Integer.MIN_VALUE) {
			reporterBuilder.closeTimeout(config.closeTimeout(), TimeUnit.SECONDS);
		}
		if (config.messageTimeout() != Integer.MIN_VALUE) {
			reporterBuilder.messageTimeout(config.messageTimeout(), TimeUnit.SECONDS);
		}
		if (config.queuedMaxBytes() != Integer.MIN_VALUE) {
			reporterBuilder.queuedMaxBytes(config.queuedMaxBytes());
		}
		if (config.queuedMaxSpans() != Integer.MIN_VALUE) {
			reporterBuilder.queuedMaxSpans(config.queuedMaxSpans());
		}
		Reporter<zipkin.Span> reporter = reporterBuilder.build();
		Tracing braveTracing = Tracing.newBuilder()
				.localServiceName(serviceName)
				.reporter(reporter)
				.build();
		tracer = BraveTracer.create(braveTracing);
	}

	/** {@inheritDoc} */
	public ActiveSpan activeSpan() {
		return tracer.activeSpan();
	}

	/** {@inheritDoc} */
	public ActiveSpan makeActive(Span span) {
		return tracer.makeActive(span);
	}

	/** {@inheritDoc} */
	public SpanBuilder buildSpan(String operationName) {
		return tracer.buildSpan(operationName);
	}

	/** {@inheritDoc} */
	public <C> SpanContext extract(Format<C> format, C carrier) {
		return tracer.extract(format, carrier);
	}

	/** {@inheritDoc} */
	public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
		tracer.inject(spanContext, format, carrier);
	}
}
