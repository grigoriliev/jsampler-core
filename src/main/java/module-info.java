module com.grigoriliev.jsampler {
	requires com.grigoriliev.jsampler.jlscp;
	requires com.grigoriliev.jsampler.juife;

	requires java.desktop;
	requires java.logging;
	requires java.prefs;
	requires java.xml;

	exports com.grigoriliev.jsampler;
	exports com.grigoriliev.jsampler.event;
	exports com.grigoriliev.jsampler.task;
	exports com.grigoriliev.jsampler.view;
}