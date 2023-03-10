/*
 *   JSampler - a front-end for LinuxSampler
 *
 *   Copyright (C) 2005-2023 Grigor Iliev <grigor@grigoriliev.com>
 *
 *   This file is part of JSampler.
 *
 *   JSampler is free software: you can redistribute it and/or modify it under
 *   the terms of the GNU General Public License as published by the Free
 *   Software Foundation, either version 3 of the License, or (at your option)
 *   any later version.
 *
 *   JSampler is distributed in the hope that it will be useful, but WITHOUT
 *   ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *   FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *   more details.
 *
 *   You should have received a copy of the GNU General Public License along
 *   with JSampler. If not, see <https://www.gnu.org/licenses/>. 
 */

package com.grigoriliev.jsampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import com.grigoriliev.jsampler.event.ListEvent;
import com.grigoriliev.jsampler.event.OrchestraEvent;
import com.grigoriliev.jsampler.juife.PDUtils;
import com.grigoriliev.jsampler.juife.Task;
import com.grigoriliev.jsampler.juife.TaskQueue;

import com.grigoriliev.jsampler.juife.event.GenericEvent;
import com.grigoriliev.jsampler.juife.event.GenericListener;
import com.grigoriliev.jsampler.juife.event.TaskEvent;
import com.grigoriliev.jsampler.juife.event.TaskListener;
import com.grigoriliev.jsampler.juife.event.TaskQueueEvent;
import com.grigoriliev.jsampler.juife.event.TaskQueueListener;

import com.grigoriliev.jsampler.task.*;
import com.grigoriliev.jsampler.view.JSChannelsPane;
import com.grigoriliev.jsampler.view.JSMainFrame;
import com.grigoriliev.jsampler.view.JSProgress;
import com.grigoriliev.jsampler.view.JSViewConfig;
import com.grigoriliev.jsampler.event.ListListener;
import com.grigoriliev.jsampler.event.OrchestraListener;

import com.grigoriliev.jsampler.task.*;

import com.grigoriliev.jsampler.jlscp.Client;
import com.grigoriliev.jsampler.jlscp.FxSend;

import com.grigoriliev.jsampler.jlscp.event.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * This class serves as a 'Control Center' of the application.
 * It also provides some fundamental routines and access to most used objects.
 * @author Grigor Iliev
 */
public class CC {
	private static Handler handler;
	private static FileOutputStream fos;
	
	private static JSViewConfig viewConfig = null;
	private static JSMainFrame mainFrame = null;
	private static JSProgress progress = null;
	
	private final static Client lsClient = new Client();
	
	private static String jSamplerHome = null;
	
	private final static TaskQueue taskQueue = new TaskQueue();
	private final static Timer timer = new Timer();
	private static TimerTask progressTimerTask = null;
	
	private static int connectionFailureCount = 0;
	
	static class ProgressTimerTask extends TimerTask {
		public void run() {
			PDUtils.runOnUiThread(new Runnable() { public void run() { run0(); } });
		}
		
		private void
		run0() { CC.getProgressIndicator().start(); }
	}
	
	public static interface Run<A> {
		public void run(A arg);
	}
	
	/** Forbids the instantiation of this class. */
	private
	CC() { }
	
	/**
	 * Returns the logger to be used for logging events.
	 * @return The logger to be used for logging events.
	 */
	public static Logger
	getLogger() {
		return Logger.getLogger (
			"com.grigoriliev.jsampler",
			"com.grigoriliev.jsampler.langprops.LogsBundle"
		);
	}
	
	/**
	 * Returns the task queue to be used for scheduling tasks
	 * for execution out of the event-dispatching thread.
	 * @return The task queue to be used for scheduling tasks
	 * for execution out of the event-dispatching thread.
	 */
	public static synchronized TaskQueue
	getTaskQueue() { return taskQueue; }
	
	/**
	 * Adds the specified task to the task queue. All task in the
	 * queue equal to the specified task are removed from the queue.
	 */
	public static synchronized void
	scheduleTask(Task t) {
		while(getTaskQueue().removeTask(t)) { }
		
		getTaskQueue().add(t);
	}
	
	/**
	 * Adds the specified task to the task queue only if the last
	 * task in the queue is not equal to <code>t</code>.
	 */
	public static synchronized void
	addTask(Task t) {
		Task[] tasks = getTaskQueue().getPendingTasks();
		if(tasks.length > 0 && tasks[tasks.length - 1].equals(t)) return;
		getTaskQueue().add(t);
	} 
	
	/**
	 * Gets the configuration of the current view.
	 */
	public static JSViewConfig
	getViewConfig() { return viewConfig; }
	
	public static JSPrefs
	preferences() { return getViewConfig().preferences(); }
	
	/**
	 * Sets the configuration of the current view.
	 */
	public static void
	setViewConfig(JSViewConfig viewConfig) { CC.viewConfig = viewConfig; }
	
	/**
	 * Returns the main window of this application.
	 * @return The main window of this application.
	 */
	public static JSMainFrame<JSChannelsPane>
	getMainFrame() { return mainFrame; }
	
	/**
	 * Sets the main window of this application.
	 * @param mainFrame The main window of this application.
	 */
	public static void
	setMainFrame(JSMainFrame mainFrame) { CC.mainFrame = mainFrame; }
	
	/**
	 * Gets the progress indicator of this application.
	 * @return The progress indicator of this application.
	 */
	public static JSProgress
	getProgressIndicator() { return progress; }
	
	/**
	 * Sets the progress indicator to be used by this application.
	 * @param progress The progress indicator to be used by this application.
	 */
	public static void
	setProgressIndicator(JSProgress progress) { CC.progress = progress; }
	
	/**
	 * Gets the absolute path to the JSampler's home location.
	 * @return The absolute path to the JSampler's home location
	 * or <code>null</code> if the JSampler's home location is not specified yet.
	 */
	public static String
	getJSamplerHome() { return jSamplerHome; }
	
	/**
	 * Sets the location of the JSampler's home.
	 * @param path The new absolute path to the JSampler's home location.
	 */
	public static void
	setJSamplerHome(String path) {
		jSamplerHome = path;
		Prefs.setJSamplerHome(jSamplerHome);
	}
	
	/**
	 * This method does the initial preparation of the application.
	 */
	protected static void
	initJSampler() {
		fos = null;
		setJSamplerHome(Prefs.getJSamplerHome());
		String s = getJSamplerHome();
		try {
			if(s != null) {
				s += File.separator + "jsampler.log";
				File f = new File(s);
				if(f.isFile()) HF.createBackup("jsampler.log", "jsampler.log.0");
				fos = new FileOutputStream(s);
			}
		} catch(Exception x) { x.printStackTrace(); }
		
		if(fos == null) handler = new StreamHandler(System.out, new SimpleFormatter());
		else handler = new StreamHandler(fos, new SimpleFormatter());
		
		handler.setLevel(Level.FINE);
		getLogger().addHandler(handler);
		getLogger().setLevel(Level.FINE);
		Logger.getLogger("com.grigoriliev.jsampler.jlscp").setLevel(Level.FINE);
		Logger.getLogger("com.grigoriliev.jsampler.jlscp").addHandler(handler);
		
		// Flushing logs on every second
		timer.schedule(new java.util.TimerTask() {
			public void
			run() { if(handler != null) handler.flush(); }
		}, 1000, 1000);
		
		getLogger().fine("CC.jsStarted");
		
		getTaskQueue().addTaskQueueListener(getHandler());
		
		getTaskQueue().start();
		
		getClient().removeChannelCountListener(getHandler());
		getClient().addChannelCountListener(getHandler());
		
		getClient().removeChannelInfoListener(getHandler());
		getClient().addChannelInfoListener(getHandler());
		
		getClient().removeFxSendCountListener(getHandler());
		getClient().addFxSendCountListener(getHandler());
		
		getClient().removeFxSendInfoListener(getHandler());
		getClient().addFxSendInfoListener(getHandler());
		
		getClient().removeStreamCountListener(getHandler());
		getClient().addStreamCountListener(getHandler());
		
		getClient().removeVoiceCountListener(getHandler());
		getClient().addVoiceCountListener(getHandler());
		
		getClient().removeTotalStreamCountListener(getHandler());
		getClient().addTotalStreamCountListener(getHandler());
		
		getClient().removeTotalVoiceCountListener(getHandler());
		getClient().addTotalVoiceCountListener(getHandler());
		
		getClient().removeAudioDeviceCountListener(audioDeviceCountListener);
		getClient().addAudioDeviceCountListener(audioDeviceCountListener);
		
		getClient().removeAudioDeviceInfoListener(audioDeviceInfoListener);
		getClient().addAudioDeviceInfoListener(audioDeviceInfoListener);
		
		getClient().removeMidiDeviceCountListener(midiDeviceCountListener);
		getClient().addMidiDeviceCountListener(midiDeviceCountListener);
		
		getClient().removeMidiDeviceInfoListener(midiDeviceInfoListener);
		getClient().addMidiDeviceInfoListener(midiDeviceInfoListener);
		
		getClient().removeMidiInstrumentMapCountListener(midiInstrMapCountListener);
		getClient().addMidiInstrumentMapCountListener(midiInstrMapCountListener);
		
		getClient().removeMidiInstrumentMapInfoListener(midiInstrMapInfoListener);
		getClient().addMidiInstrumentMapInfoListener(midiInstrMapInfoListener);
		
		getClient().removeMidiInstrumentCountListener(getHandler());
		getClient().addMidiInstrumentCountListener(getHandler());
		
		getClient().removeMidiInstrumentInfoListener(getHandler());
		getClient().addMidiInstrumentInfoListener(getHandler());
		
		getClient().removeGlobalInfoListener(getHandler());
		getClient().addGlobalInfoListener(getHandler());
		
		getClient().removeChannelMidiDataListener(getHandler());
		getClient().addChannelMidiDataListener(getHandler());
		
		getClient().removeSendEffectChainCountListener(getHandler());
		getClient().addSendEffectChainCountListener(getHandler());
		
		getClient().removeSendEffectChainInfoListener(getHandler());
		getClient().addSendEffectChainInfoListener(getHandler());
		
		getClient().removeEffectInstanceInfoListener(getHandler());
		getClient().addEffectInstanceInfoListener(getHandler());
		
		CC.addConnectionEstablishedListener(new GenericListener() {
			public void
			jobDone(GenericEvent e) {
				connectionFailureCount = 0;
			}
		});
	}
	
	private final static OrchestraListModel orchestras = new DefaultOrchestraListModel();
	
	/**
	 * Returns a list containing all available orchestras.
	 * @return A list containing all available orchestras.
	 */
	public static OrchestraListModel
	getOrchestras() { return orchestras; }
	
	private final static ServerList servers = new ServerList();
	
	/** Returns the server list. */
	public static ServerList
	getServerList() { return servers; }
	
	private static ServerListListener serverListListener = new ServerListListener();
	
	private static class ServerListListener implements GenericListener {
		public void
		jobDone(GenericEvent e) { saveServerList(); }
	}
	
	private static final 
	
	LostFilesModel lostFilesModel = new LostFilesModel();
	
	public static LostFilesModel
	getLostFilesModel() { return lostFilesModel; }
	
	/**
	 * Loads the orchestras described in <code>&lt;jsampler_home&gt;/orchestras.xml</code>.
	 * If file with name <code>orchestras.xml.bkp</code> exist in the JSampler's home
	 * directory, this means that the last save has failed. In that case a recovery file
	 * <code>orchestras.xml.rec</code> is created and a recovery procedure
	 * will be initiated.
	 */
	public static void
	loadOrchestras() {
		if(getJSamplerHome() == null) return;
		
		try {
			String s = getJSamplerHome();
			
			File f = new File(s + File.separator + "orchestras.xml.bkp");
			if(f.isFile()) HF.createBackup("orchestras.xml.bkp", "orchestras.xml.rec");
			
			FileInputStream fis;
			fis = new FileInputStream(s + File.separator + "orchestras.xml");
			
			loadOrchestras(fis);
			fis.close();
		} catch(Exception x) {
			getLogger().log(Level.INFO, HF.getErrorMessage(x), x);
		}
		
		getOrchestras().addOrchestraListListener(getHandler());
	}
	
	
	private static void
	loadOrchestras(InputStream in) {
		Document doc = DOMUtils.readObject(in);
		
		try { getOrchestras().readObject(doc.getDocumentElement()); }
		catch(Exception x) {
			getViewConfig().showErrorMessage(x, "Loading orchestras: ");
			return;
		}
		
		for(int i = 0; i < getOrchestras().getOrchestraCount(); i++) {
			getOrchestras().getOrchestra(i).addOrchestraListener(getHandler());
		}
	}
	
	private static void
	saveOrchestras() {
		try {
			String s = getJSamplerHome();
			if(s == null) return;
			
			HF.createBackup("orchestras.xml", "orchestras.xml.bkp");
			
			FileOutputStream fos2;
			fos2 = new FileOutputStream(s + File.separator + "orchestras.xml", false);
			
			Document doc = DOMUtils.createEmptyDocument();
		
			Node node = doc.createElement("temp");
			doc.appendChild(node);
			
			getOrchestras().writeObject(doc, doc.getDocumentElement());
			
			doc.replaceChild(node.getFirstChild(), node);
		
			DOMUtils.writeObject(doc, fos2);
			
			fos2.close();
			
			HF.deleteFile("orchestras.xml.bkp");
		} catch(Exception x) {
			getViewConfig().showErrorMessage(x, "Saving orchestras: ");
			return;
		}
	}
	
	/**
	 * Loads the servers' info described in <code>&lt;jsampler_home&gt;/servers.xml</code>.
	 * If file with name <code>servers.xml.bkp</code> exist in the JSampler's home
	 * directory, this means that the last save has failed. In that case a recovery file
	 * <code>servers.xml.rec</code> is created and a recovery procedure
	 * will be initiated.
	 */
	public static void
	loadServerList() {
		if(getJSamplerHome() == null) return;
		
		try {
			String s = getJSamplerHome();
			
			File f = new File(s + File.separator + "servers.xml.bkp");
			if(f.isFile()) HF.createBackup("servers.xml.bkp", "servers.xml.rec");
			
			FileInputStream fis;
			fis = new FileInputStream(s + File.separator + "servers.xml");
			
			loadServerList(fis);
			fis.close();
		} catch(Exception x) {
			getLogger().log(Level.INFO, HF.getErrorMessage(x), x);
		}
		
		getServerList().addChangeListener(serverListListener);
		
		/* We should have at least one server to connect. */
		if(getServerList().getServerCount() == 0) {
			Server server = new Server();
			server.setName("127.0.0.1:8888");
			server.setAddress("127.0.0.1");
			server.setPort(8888);
			getServerList().addServer(server);
		}
	}
	
	
	private static void
	loadServerList(InputStream in) {
		Document doc = DOMUtils.readObject(in);
		
		try { getServerList().readObject(doc.getDocumentElement()); }
		catch(Exception x) {
			getViewConfig().showErrorMessage(x, "Loading server list: ");
			return;
		}
	}
	
	private static void
	saveServerList() {
		try {
			String s = getJSamplerHome();
			if(s == null) return;
			
			HF.createBackup("servers.xml", "servers.xml.bkp");
			
			FileOutputStream fos2;
			fos2 = new FileOutputStream(s + File.separator + "servers.xml", false);
			
			Document doc = DOMUtils.createEmptyDocument();
		
			Node node = doc.createElement("temp");
			doc.appendChild(node);
			
			getServerList().writeObject(doc, doc.getDocumentElement());
			
			doc.replaceChild(node.getFirstChild(), node);
		
			DOMUtils.writeObject(doc, fos2);
			
			fos2.close();
			
			HF.deleteFile("servers.xml.bkp");
		} catch(Exception x) {
			getViewConfig().showErrorMessage(x, "Saving server list: ");
			return;
		}
	}
	
	/**
	 * The exit point of the application which ensures clean exit with default exit status 0.
	 *  @see #cleanExit(int i)
	 */
	public static void
	cleanExit() { cleanExit(0); }
	
	/**
	 * The exit point of the application which ensures clean exit.
	 * @param i The exit status.
	 */
	public static void
	cleanExit(int i) {
		getLogger().fine("CC.jsEnded");
		try { getClient().disconnect(); } // FIXME: this might block the EDT
		catch(Exception x) { x.printStackTrace(); }
		if(backendProcess != null) backendProcess.destroy();
		backendProcess = null;
		fireBackendProcessEvent();
		System.exit(i);
	}
	
	/**
	 * Gets the <code>Client</code> object that is used to communicate with the backend.
	 * @return The <code>Client</code> object that is used to communicate with the backend.
	 */
	public static Client
	getClient() { return lsClient; }
	
	private static final Vector<GenericListener> listeners = new Vector<GenericListener>();
	
	/**
	 * Registers the specified listener to be notified (outside the UI thread)
	 * when reconnecting to LinuxSampler.
	 * @param l The <code>ActionListener</code> to register.
	 */
	public static void
	addReconnectListener(GenericListener l) { listeners.add(l); }
	
	/**
	 * Removes the specified listener.
	 * @param l The <code>ActionListener</code> to remove.
	 */
	public static void
	removeReconnectListener(GenericListener l) { listeners.remove(l); }
	
	private static void
	fireReconnectEvent() {
		GenericEvent e = new GenericEvent(CC.class);
		for(GenericListener l : listeners) l.jobDone(e);
	}
	
	private static final Vector<GenericListener> ceListeners = new Vector<GenericListener>();
	
	/**
	 * Registers the specified listener to be notified (from the UI thread) when
	 * jsampler is connected successfully to LinuxSampler.
	 * @param l The <code>GenericListener</code> to register.
	 */
	public static void
	addConnectionEstablishedListener(GenericListener l) { ceListeners.add(l); }
	
	/**
	 * Removes the specified listener.
	 * @param l The <code>GenericListener</code> to remove.
	 */
	public static void
	removeConnectionEstablishedListener(GenericListener l) { ceListeners.remove(l); }
	
	private static void
	fireConnectionEstablishedEvent() {
		GenericEvent e = new GenericEvent(CC.class);
		for(GenericListener l : ceListeners) l.jobDone(e);
	}
	
	private static final SamplerModel samplerModel = new DefaultSamplerModel();
	
	/**
	 * Gets the sampler model.
	 * @return The sampler model.
	 */
	public static SamplerModel
	getSamplerModel() { return samplerModel; }
	
	/**
	 * Connects to LinuxSampler.
	 * The connection can be done asynchronously.
	 */
	public static void
	connect() { initSamplerModel(); }
	
	/**
	 * Reconnects to LinuxSampler.
	 */
	public static void
	reconnect() { initSamplerModel(getCurrentServer()); }
	
	private static Server currentServer = null;
	
	/**
	 * Gets the server, to which the frontend is going to connect
	 * or is already connected.
	 */
	public static Server
	getCurrentServer() { return currentServer; }
	
	/**
	 * Sets the current server.
	 */
	public static void
	setCurrentServer(Server server) {
		if(server == currentServer) return;
		connectionFailureCount = 0;
		currentServer = server;
	}
	
	/**
	 * Sets the LSCP client's read timeout.
	 * @param timeout The new timeout value (in seconds).
	 */
	public static void
	setClientReadTimeout(int timeout) {
		getTaskQueue().add(new Global.SetClientReadTimeout(timeout));
	}
	
	/**
	 * This method updates the information about the backend state.
	 * The update can be done asynchronously.
	 */
	private static void
	initSamplerModel() {
		getMainFrame().getServer(new Run<Server>() {
			public void
			run(Server srv) {
				if(srv == null) return;
				initSamplerModel(srv);
			}
		});
	}
	
	/**
	 * This method updates the information about the backend state.
	 */
	private static void
	initSamplerModel(Server srv) {
		setCurrentServer(srv);
		final SetServerAddress ssa = new SetServerAddress(srv.getAddress(), srv.getPort());
		
		final DefaultSamplerModel model = (DefaultSamplerModel)getSamplerModel();
		
		final Global.GetServerInfo gsi = new Global.GetServerInfo();
		gsi.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(gsi.doneWithErrors()) return;
				
				model.setServerInfo(gsi.getResult());
				
				if(CC.getViewConfig().getInstrumentsDbSupport()) {
					CC.getViewConfig().initInstrumentsDbTreeModel();
				}
			}
		});
		
		final Audio.GetDrivers gaod = new Audio.GetDrivers();
		gaod.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(!gaod.doneWithErrors())
					model.setAudioOutputDrivers(gaod.getResult());
			}
		});
		
		final Global.GetEngines ge = new Global.GetEngines();
		ge.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(!ge.doneWithErrors()) model.setEngines(ge.getResult());
			}
		});
		
		final Midi.GetDrivers gmid = new Midi.GetDrivers();
		gmid.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(!gmid.doneWithErrors())
					model.setMidiInputDrivers(gmid.getResult());
			}
		});
		
		final Global.GetVolume gv = new Global.GetVolume();
		gv.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(!gv.doneWithErrors())
					model.setVolume(gv.getResult());
			}
		});
		
		final Midi.GetInstrumentMaps mgim = new Midi.GetInstrumentMaps();
		mgim.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(mgim.doneWithErrors()) return;
				model.removeAllMidiInstrumentMaps();
				
				for(MidiInstrumentMap map : mgim.getResult()) {
					model.addMidiInstrumentMap(map);
				}
			}
		});
		
		final Global.GetEffects gfx = new Global.GetEffects();
		gfx.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(!gfx.doneWithErrors()) model.getEffects().setEffects(gfx.getResult());
			}
		});
		
		final UpdateChannels uc = new UpdateChannels();
		uc.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				for(SamplerChannelModel c : model.getChannels()) {
					if(c.getChannelInfo().getEngine() == null) continue;
					
					Channel.GetFxSends gfs = new Channel.GetFxSends();
					gfs.setChannel(c.getChannelId());
					gfs.addTaskListener(new GetFxSendsListener());
					getTaskQueue().add(gfs);
				}
				
				// TODO: This should be done after the fx sends are set
				//CC.getSamplerModel().setModified(false);
			}
		});
		
		
		final Global.Connect cnt = new Global.Connect();
		boolean b = preferences().getBoolProperty(JSPrefs.LAUNCH_BACKEND_LOCALLY);
		if(b && srv.isLocal() && backendProcess == null) cnt.setSilent(true);
		cnt.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				if(cnt.doneWithErrors()) {
					onConnectFailure();
					return;
				}
				
				// Don't change order!!!
				getTaskQueue().add(gsi);
				getTaskQueue().add(gaod);
				getTaskQueue().add(gmid);
				getTaskQueue().add(ge);
				getTaskQueue().add(gv);
				getTaskQueue().add(mgim);
				getTaskQueue().add(gfx);
				getTaskQueue().add(new Midi.UpdateDevices());
				getTaskQueue().add(new Audio.UpdateDevices());
				addTask(uc);
				getTaskQueue().add(new Global.UpdateSendEffectChains());
				
				int vl = preferences().getIntProperty(JSPrefs.GLOBAL_VOICE_LIMIT);
				int sl = preferences().getIntProperty(JSPrefs.GLOBAL_STREAM_LIMIT);
				
				getTaskQueue().add(new Global.SetPolyphony(vl, sl));
				
				fireConnectionEstablishedEvent();
			}
		});
		
		ssa.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				int t = preferences().getIntProperty(JSPrefs.SOCKET_READ_TIMEOUT);
				CC.setClientReadTimeout(t * 1000);
				CC.getTaskQueue().add(cnt);
			}
		});
		
		getSamplerModel().reset();
		getViewConfig().resetInstrumentsDbTreeModel();
		
		getTaskQueue().removePendingTasks();
		getTaskQueue().add(ssa);
		
		fireReconnectEvent();
	}
	
	private static void
	onConnectFailure() {
		connectionFailureCount++;
		if(connectionFailureCount > 50) { // to prevent eventual infinite loop
			getLogger().warning("Reached maximum number of connection failures");
			return;
		}
		
		try {
			if(launchBackend()) {
				int i = preferences().getIntProperty(JSPrefs.BACKEND_LAUNCH_DELAY);
				if(i < 1) {
					initSamplerModel(getCurrentServer());
					return;
				}
				
				LaunchBackend lb = new LaunchBackend(i, getBackendMonitor());
				//CC.getTaskQueue().add(lb);
				new Thread(lb).start();
				return;
			}
		} catch(Exception x) {
			final String s = JSI18n.i18n.getError("CC.failedToLaunchBackend");
			CC.getLogger().log(Level.INFO, s, x);
			
			PDUtils.runOnUiThread(new Runnable() {
				public void
				run() { getViewConfig().showErrorMessage(s); }
			});
			return;
		}
		
		retryToConnect();
	}
	
	private static void
	retryToConnect() {
		PDUtils.runOnUiThread(new Runnable() {
			public void
			run() { changeBackend(); }
		});
	}
	
	public static void
	changeBackend() {
		getMainFrame().getServer(new Run<Server>() {
			public void
			run(Server srv) {
				if(srv == null) return;
				connectionFailureCount = 0; // cleared because this change is due to user interaction
				initSamplerModel(srv);
			}
		}, true);
	}
	
	private static final Vector<GenericListener> pListeners = new Vector<GenericListener>();
	
	/**
	 * Registers the specified listener to be notified (from UI thread) when
	 * backend process is created/terminated.
	 * @param l The <code>ActionListener</code> to register.
	 */
	public static void
	addBackendProcessListener(GenericListener l) { pListeners.add(l); }
	
	/**
	 * Removes the specified listener.
	 * @param l The <code>ActionListener</code> to remove.
	 */
	public static void
	removeBackendProcessListener(GenericListener l) { pListeners.remove(l); }
	
	private static void
	fireBackendProcessEvent() {
		GenericEvent e = new GenericEvent(CC.class);
		for(GenericListener l : pListeners) l.jobDone(e);
	}
	
	private static Process backendProcess = null;
	
	public static Process
	getBackendProcess() { return backendProcess; }
	
	private static final Object backendMonitor = new Object();
	
	public static Object
	getBackendMonitor() { return backendMonitor; }
	
	private static boolean
	launchBackend() throws Exception {
		if(backendProcess != null) {
			try {
				int i = backendProcess.exitValue();
				getLogger().info("Backend exited with exit value " + i);
				backendProcess = null;
				fireBackendProcessEvent();
			} catch(IllegalThreadStateException x) { return false; }
		}
		
		if(!preferences().getBoolProperty(JSPrefs.LAUNCH_BACKEND_LOCALLY)) return false;
		if(connectionFailureCount > 1) return false;
		
		Server  s = getCurrentServer();
		if(s != null && s.isLocal()) {
			String cmd = preferences().getStringProperty(JSPrefs.BACKEND_LAUNCH_COMMAND);
			backendProcess = Runtime.getRuntime().exec(cmd);
			fireBackendProcessEvent();
			return true;
		}
		
		return false;
	}
	
	private static class GetFxSendsListener implements TaskListener {
		public void
		taskPerformed(TaskEvent e) {
			Channel.GetFxSends gfs = (Channel.GetFxSends)e.getSource();
			if(gfs.doneWithErrors()) return;
			SamplerChannelModel m = getSamplerModel().getChannelById(gfs.getChannel());
			m.removeAllFxSends();
			
			for(FxSend fxs : gfs.getResult()) m.addFxSend(fxs);
		}
	}
	
	public static void
	scheduleInTaskQueue(final Runnable r) {
		Task dummy = new Global.DummyTask();
		dummy.addTaskListener(new TaskListener() {
			public void
			taskPerformed(TaskEvent e) {
				PDUtils.runOnUiThread(r);
			}
		});
		
		getTaskQueue().add(dummy);
	}
	
	public static boolean
	verifyConnection() {
		if(getCurrentServer() == null) {
			getViewConfig().showErrorMessage(JSI18n.i18n.getError("CC.notConnected"));
			return false;
		}
		
		return true;
	}

	public static boolean
	isMacOS() {
		return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	}
	
	
	private final static EventHandler eventHandler = new EventHandler();
	
	private static EventHandler
	getHandler() { return eventHandler; }
	
	private static class EventHandler implements ChannelCountListener, ChannelInfoListener,
		FxSendCountListener, FxSendInfoListener, StreamCountListener, VoiceCountListener,
		TotalStreamCountListener, TotalVoiceCountListener, TaskQueueListener,
		OrchestraListener, ListListener<OrchestraModel>, MidiInstrumentCountListener,
		MidiInstrumentInfoListener, GlobalInfoListener, ChannelMidiDataListener,
		SendEffectChainCountListener, SendEffectChainInfoListener, EffectInstanceInfoListener {
		
		/** Invoked when the number of channels has changed. */
		@Override
		public void
		channelCountChanged( ChannelCountEvent e) {
			if(e.getChannelCount() == 0) {
				/*
				 * This special case is handled because this might be due to
				 * loading a lscp script containing sampler view configuration.
				 */
				CC.getSamplerModel().removeAllChannels();
				return;
			}
			addTask(new UpdateChannels());
		}
		
		/** Invoked when changes to the sampler channel has occured. */
		@Override
		public void
		channelInfoChanged(ChannelInfoEvent e) {
			/*
			 * Because of the rapid notification flow when instrument is loaded
			 * we need to do some optimization to decrease the traffic.
			 */
			boolean b = true;
			Task[] tS = getTaskQueue().getPendingTasks();
			
			for(int i = tS.length - 1; i >= 0; i--) {
				Task t = tS[i];
				
				if(t instanceof Channel.UpdateInfo) {
					Channel.UpdateInfo cui = (Channel.UpdateInfo)t;
					if(cui.getChannelId() == e.getSamplerChannel()) return;
				} else {
					b = false;
					break;
				}
			}
			
			if(b) {
				Task t = getTaskQueue().getRunningTask();
				if(t instanceof Channel.UpdateInfo) {
					Channel.UpdateInfo cui = (Channel.UpdateInfo)t;
					if(cui.getChannelId() == e.getSamplerChannel()) return;
				}
			}
			
			
			getTaskQueue().add(new Channel.UpdateInfo(e.getSamplerChannel()));
		}
		
		/**
		 * Invoked when the number of effect sends
		 * on a particular sampler channel has changed.
		 */
		@Override
		public void
		fxSendCountChanged(FxSendCountEvent e) {
			getTaskQueue().add(new Channel.UpdateFxSends(e.getChannel()));
		}
		
		/**
		 * Invoked when the settings of an effect sends are changed.
		 */
		@Override
		public void
		fxSendInfoChanged(FxSendInfoEvent e) {
			Task t = new Channel.UpdateFxSendInfo(e.getChannel(), e.getFxSend());
			getTaskQueue().add(t);
		}
		
		/**
		 * Invoked when the number of active disk
		 * streams in a specific sampler channel has changed.
		 */
		@Override
		public void
		streamCountChanged(StreamCountEvent e) {
			SamplerChannelModel scm = 
				getSamplerModel().getChannelById(e.getSamplerChannel());
			
			if(scm == null) {
				CC.getLogger().log (
					Level.WARNING,
					"CC.unknownChannel!",
					e.getSamplerChannel()
				);
				
				return;
			}
			
			scm.setStreamCount(e.getStreamCount());
		}
		
		/**
		 * Invoked when the number of active voices
		 * in a specific sampler channel has changed.
		 */
		@Override
		public void
		voiceCountChanged(VoiceCountEvent e) {
			SamplerChannelModel scm = 
				getSamplerModel().getChannelById(e.getSamplerChannel());
			
			if(scm == null) {
				CC.getLogger().log (
					Level.WARNING,
					"CC.unknownChannel!",
					e.getSamplerChannel()
				);
				
				return;
			}
			
			scm.setVoiceCount(e.getVoiceCount());
		}
		
		/** Invoked when the total number of active streams has changed. */
		@Override
		public void
		totalStreamCountChanged(TotalStreamCountEvent e) {
			getSamplerModel().updateActiveStreamsInfo(e.getTotalStreamCount());
		}
		
		/** Invoked when the total number of active voices has changed. */
		@Override
		public void
		totalVoiceCountChanged(TotalVoiceCountEvent e) {
			scheduleTask(new Global.UpdateTotalVoiceCount());
		}
		
		/** Invoked when the number of MIDI instruments in a MIDI instrument map is changed. */
		@Override
		public void
		instrumentCountChanged(MidiInstrumentCountEvent e) {
			scheduleTask(new Midi.UpdateInstruments(e.getMapId()));
		}
		
		/** Invoked when a MIDI instrument in a MIDI instrument map is changed. */
		@Override
		public void
		instrumentInfoChanged(MidiInstrumentInfoEvent e) {
			Task t = new Midi.UpdateInstrumentInfo (
				e.getMapId(), e.getMidiBank(), e.getMidiProgram()
			); 
			getTaskQueue().add(t);
				
		}
		
		/** Invoked when the global volume of the sampler is changed. */
		@Override
		public void
		volumeChanged(GlobalInfoEvent e) {
			getSamplerModel().setVolume(e.getVolume());
		}
		
		@Override
		public void
		voiceLimitChanged(GlobalInfoEvent e) { }
		
		@Override
		public void
		streamLimitChanged(GlobalInfoEvent e) { }
		
		/**
		 * Invoked to indicate that the state of a task queue is changed.
		 * This method is invoked only from the UI thread.
		 */
		@Override
		public void
		stateChanged(TaskQueueEvent e) {
			switch(e.getEventID()) {
			case TASK_FETCHED:
				getProgressIndicator().setString (
					((Task)e.getSource()).getDescription()
				);
				break;
			case TASK_DONE:
				EnhancedTask t = (EnhancedTask)e.getSource();
				if(t.doneWithErrors() && !t.isSilent()) {
					if(t.getErrorCode() == t.SOCKET_ERROR) {
						getMainFrame().handleConnectionFailure();
					} else if(!t.isStopped()) {
						showError(t);
					}
				}
				break;
			case NOT_IDLE:
				if(progressTimerTask != null) {
					getLogger().warning("progressTimerTask != null - this is a bug!");
				}
				progressTimerTask = new ProgressTimerTask();
				timer.schedule(progressTimerTask, 2000);
				break;
			case IDLE:
				if(progressTimerTask != null) {
					progressTimerTask.cancel();
					progressTimerTask = null;
				}
				getProgressIndicator().stop();
				break;
			}
		}
		
		private void
		showError(final Task t) {
			PDUtils.runOnUiThread(new Runnable() {
				public void
				run() {
					if(t.getErrorDetails() == null) {
						getViewConfig().showErrorMessage(t.getErrorMessage());
					} else {
						getMainFrame().showDetailedErrorMessage (
							t.getErrorMessage(),
							t.getErrorDetails()
						);
					}
				}
			});
		}
		
		/** Invoked when the name of orchestra is changed. */
		@Override
		public void
		nameChanged(OrchestraEvent e) { saveOrchestras(); }
	
		/** Invoked when the description of orchestra is changed. */
		@Override
		public void
		descriptionChanged(OrchestraEvent e) { saveOrchestras(); }
	
		/** Invoked when an instrument is added to the orchestra. */
		@Override
		public void
		instrumentAdded(OrchestraEvent e) { saveOrchestras(); }
	
		/** Invoked when an instrument is removed from the orchestra. */
		@Override
		public void
		instrumentRemoved(OrchestraEvent e) { saveOrchestras(); }
	
		/** Invoked when the settings of an instrument are changed. */
		@Override
		public void
		instrumentChanged(OrchestraEvent e) { saveOrchestras(); }
		
		/** Invoked when an orchestra is added to the orchestra list. */
		@Override
		public void
		entryAdded(ListEvent<OrchestraModel> e) {
			e.getEntry().addOrchestraListener(getHandler());
			saveOrchestras();
		}
	
		/** Invoked when an orchestra is removed from the orchestra list. */
		@Override
		public void
		entryRemoved(ListEvent<OrchestraModel> e) {
			e.getEntry().removeOrchestraListener(getHandler());
			saveOrchestras();
		}
		
		/**
		 * Invoked when MIDI data arrives.
		 */
		@Override
		public void
		midiDataArrived(final ChannelMidiDataEvent e) {
			try {
				PDUtils.runOnUiThreadAndWait(new Runnable() {
					public void
					run() { fireChannelMidiDataEvent(e); }
				});
			} catch(Exception x) {
				CC.getLogger().log(Level.INFO, "Failed!", x);
			}
		}
		
		@Override
		public void
		sendEffectChainCountChanged(SendEffectChainCountEvent e) {
			getTaskQueue().add(new Audio.UpdateSendEffectChains(e.getAudioDeviceId()));
		}
		
		@Override
		public void
		sendEffectChainInfoChanged(SendEffectChainInfoEvent e) {
			if(e.getInstanceCount() == -1) return;
			
			getTaskQueue().add (
				new Audio.UpdateEffectInstances(e.getAudioDeviceId(), e.getChainId())
			);
		}
		
		@Override
		public void
		effectInstanceInfoChanged(EffectInstanceInfoEvent e) {
			getTaskQueue().add(new Audio.UpdateEffectInstanceInfo(e.getEffectInstanceId()));
		}
	}
	
	private static void
	fireChannelMidiDataEvent(ChannelMidiDataEvent e) {
		SamplerChannelModel chn;
		chn = getSamplerModel().getChannelById(e.getChannelId());
		if(chn == null) {
			CC.getLogger().info("Unknown channel ID: " + e.getChannelId());
		}
		
		((DefaultSamplerChannelModel)chn).fireMidiDataEvent(e);
	}
	
	private static final AudioDeviceCountListener audioDeviceCountListener = 
		new AudioDeviceCountListener();
	
	private static class AudioDeviceCountListener implements ItemCountListener {
		/** Invoked when the number of audio output devices has changed. */
		@Override
		public void
		itemCountChanged(ItemCountEvent e) {
			getTaskQueue().add(new Audio.UpdateDevices());
		}
	}
	
	private static final AudioDeviceInfoListener audioDeviceInfoListener = 
		new AudioDeviceInfoListener();
	
	private static class AudioDeviceInfoListener implements ItemInfoListener {
		/** Invoked when the audio output device's settings are changed. */
		@Override
		public void
		itemInfoChanged(ItemInfoEvent e) {
			getTaskQueue().add(new Audio.UpdateDeviceInfo(e.getItemID()));
		}
	}
	
	private static final MidiDeviceCountListener midiDeviceCountListener = 
		new MidiDeviceCountListener();
	
	private static class MidiDeviceCountListener implements ItemCountListener {
		/** Invoked when the number of MIDI input devices has changed. */
		public void
		itemCountChanged(ItemCountEvent e) {
			getTaskQueue().add(new Midi.UpdateDevices());
		}
	}
	
	private static final MidiDeviceInfoListener midiDeviceInfoListener = 
		new MidiDeviceInfoListener();
	
	private static class MidiDeviceInfoListener implements ItemInfoListener {
		/** Invoked when the MIDI input device's settings are changed. */
		@Override
		public void
		itemInfoChanged(ItemInfoEvent e) {
			getTaskQueue().add(new Midi.UpdateDeviceInfo(e.getItemID()));
		}
	}
	
	private static final MidiInstrMapCountListener midiInstrMapCountListener = 
		new MidiInstrMapCountListener();
	
	private static class MidiInstrMapCountListener implements ItemCountListener {
		/** Invoked when the number of MIDI instrument maps is changed. */
		@Override
		public void
		itemCountChanged(ItemCountEvent e) {
			getTaskQueue().add(new Midi.UpdateInstrumentMaps());
		}
	}
	
	private static final MidiInstrMapInfoListener midiInstrMapInfoListener = 
		new MidiInstrMapInfoListener();
	
	private static class MidiInstrMapInfoListener implements ItemInfoListener {
		/** Invoked when the MIDI instrument map's settings are changed. */
		@Override
		public void
		itemInfoChanged(ItemInfoEvent e) {
			getTaskQueue().add(new Midi.UpdateInstrumentMapInfo(e.getItemID()));
		}
	}
}
