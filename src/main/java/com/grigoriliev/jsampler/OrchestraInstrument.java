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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *
 * @author Grigor Iliev
 */
public class OrchestraInstrument extends Resource implements com.grigoriliev.jsampler.jlscp.Instrument {
	private String path = null;
	private int instrumentIndex = 0;
	private String engine = "GIG";
	
	/**
	 * Creates a new instance of <code>OrchestraInstrument</code>.
	 */
	public OrchestraInstrument() {
	}
	
	/**
	 * Returns the absolute pathname of the instrument location.
	 * @return The absolute pathname of the instrument location.
	 */
	public String
	getFilePath() { return path; }
	
	/**
	 * Sets the absolute pathname of the instrument location.
	 * @param path Specifies the absolute pathname of the instrument location.
	 */
	public void
	setFilePath(String path) {
		this.path = path;
		fireChangeEvent();
	}
	
	/**
	 * Returns the index of the instrument in the instrument file.
	 * @return The index of the instrument in the instrument file.
	 */
	public int
	getInstrumentIndex() { return instrumentIndex; }
	
	/**
	 * Sets the index of the instrument in the instrument file.
	 * @param idx The index of the instrument in the instrument file.
	 */
	public void
	setInstrumentIndex(int idx) {
		instrumentIndex = idx;
		fireChangeEvent();
	}
	
	/**
	 * Gets the engine to be used for loading this instrument.
	 * @return The engine to be used for loading this instrument.
	 */
	public String
	getEngine() { return engine; }
	
	/**
	 * Sets the engine to be used for loading this instrument.
	 */
	public void
	setEngine(String engine) {
		this.engine = engine;
		fireChangeEvent();
	}
	
	public String
	getFormatFamily() { return null; }
	
	public String
	getFormatVersion() { return null; }
	
	public Integer[]
	getKeyMapping() { return null; }
	
	public Integer[]
	getKeyswitchMapping() { return null; }
	
	/**
	 * Returns the name of this instrument.
	 * @return The name of this instrument.
	 */
	public String
	toString() { return getName(); }
	
	private final StringBuffer sb = new StringBuffer();
	
	/**
	 * Gets a string representation of this
	 * instrument appropriate for Drag & Drop operations.
	 * @return A string representation of this
	 * instrument appropriate for Drag & Drop operations.
	 * @see #isDnDString
	 */
	public String
	getDnDString() {
		sb.setLength(0);
		sb.append("[Instrument Definition]\n");
		sb.append(getName()).append("\n");
		sb.append("\n");
		sb.append(getDescription()).append("\n");
		sb.append(getFilePath()).append("\n");
		sb.append(getInstrumentIndex()).append("\n");
		
		return sb.toString();
	}
	
	/**
	 * Sets the instrument properties provided by the specified
	 * Drag & Drop string representation.
	 * @param s String providing Drag & Drop string representation of an instrument.
	 * @throws IllegalArgumentException If the specified string is not
	 * a Drag & Drop string representation of an instrument.
	 * @see #getDnDString
	 */
	public void
	setDnDString(String s) {
		if(!isDnDString(s)) throw new IllegalArgumentException("Not a DnD string");
		
		String[] args = s.split("\n");
		if(args.length < 6) throw new IllegalArgumentException("Not a DnD string");
		
		setName(args[1]);
		setDescription(args[3]);
		setFilePath(args[4]);
		
		try { setInstrumentIndex(Integer.parseInt(args[5])); }
		catch(Exception x) {
			throw new IllegalArgumentException("Not a DnD string", x);
		}
	}
	
	/**
	 * Determines whether the specified string is
	 * a Drag & Drop representation of an instrument.
	 * @param s The string to be checked.
	 * @return <code>true</code> if the specified string is
	 * a Drag & Drop representation of an instrument, <code>false</code> otherwise.
	 */
	public static boolean
	isDnDString(String s) {
		if(s == null) return false;
		return s.startsWith("[Instrument Definition]\n");
	}
	
	/**
	 * Reads and sets the instrument properties by the supplied <code>node</code>.
	 * @param node The node providing the instrument properties.
	 * @throws IllegalArgumentException If an error occurs while
	 * reading the instrument properties.
	 */
	public void
	readObject(Node node) {
		if(
			node.getNodeType() != Node.ELEMENT_NODE ||
			!(node.getNodeName().equals("instrument"))
		) {
			throw new IllegalArgumentException("Not an instrument node!");
		}
		
		NamedNodeMap nnm = node.getAttributes();
		Node n = nnm.getNamedItem("name");
		if(n == null) {
			throw new IllegalArgumentException("The instrument name is undefined!");
		}
		DOMUtils.validateTextContent(n);
		setName(n.getFirstChild().getNodeValue());
		
		String s = null;
		NodeList nl = node.getChildNodes();
		
		for(int i = 0; i < nl.getLength(); i++) {
			node = nl.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE) continue;
			
			s = node.getNodeName();
			if(s.equals("description")) {
				if(node.hasChildNodes()) {
					DOMUtils.validateTextContent(node);
					setDescription(node.getFirstChild().getNodeValue());
				}
			} else if(s.equals("path")) {
				DOMUtils.validateTextContent(node);
				setFilePath(node.getFirstChild().getNodeValue());
			} else if(s.equals("instrument-index")) {
				DOMUtils.validateTextContent(node);
				try {
					int j;
					j = Integer.parseInt(node.getFirstChild().getNodeValue());
					setInstrumentIndex(j);
				} catch(NumberFormatException x) {
					throw new IllegalArgumentException("Not a number");
				}
			} else if(s.equals("engine")) {
				DOMUtils.validateTextContent(node);
				setEngine(node.getFirstChild().getNodeValue());
			} else {	// Unknown content
				CC.getLogger().info ("Unknown field: " + s);
			}
		}
	}
	
	/**
	 * Writes the instrument properties to the
	 * specified node of document <code>doc</code>.
	 * @param doc The document containing <code>node</code>.
	 * @param node Specifies the node where the instrument properties
	 * should be written.
	 */
	public void
	writeObject(Document doc, Node node) {
		Element el = doc.createElement("instrument");
		el.setAttribute("name", getName());
		node.appendChild(el);
				
		node = el;
		
		el = doc.createElement("description");
		el.appendChild(doc.createTextNode(getDescription()));
		node.appendChild(el);
		
		el = doc.createElement("path");
		el.appendChild(doc.createTextNode(getFilePath()));
		node.appendChild(el);
		
		el = doc.createElement("instrument-index");
		el.appendChild(doc.createTextNode(String.valueOf(getInstrumentIndex())));
		node.appendChild(el);
		
		if (getEngine() != null) {
			el = doc.createElement("engine");
			el.appendChild(doc.createTextNode(getEngine()));
			node.appendChild(el);
		}
	}
}
