/*
 * Copyrights     : CNRS
 * Author         : Oleg Lodygensky
 * Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
 * Web            : http://www.xtremweb-hep.org
 *
 *      This file is part of XtremWeb-HEP.
 *
 *    XtremWeb-HEP is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    XtremWeb-HEP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package xtremweb.common;

/**
 * Version.java
 *
 * Assumes that all the version have the same number of digits
 */
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class Version extends XMLable {
	private String version;
	private String build;

	public Version() {
		setXMLTag("Version");
	}

	public Version(final String v) {
		this();

		StringTokenizer sk = new StringTokenizer(v, "-");
		version = sk.nextToken();
		try {
			build = sk.nextToken();
		} catch (final Exception e) {
		}
	}

	/**
	 * This calls this() and fromStrings(ver,br,bu)
	 */
	public Version(final String ver, final String bu) {
		this();
		fromStrings(ver, bu);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 *
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 */
	public Version(final DataInputStream input) throws IOException, SAXException {
		this();
		final XMLReader reader = new XMLReader(this);
		try {
			reader.read(input);
		} catch (final InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This retreives version, branch and build from parameters
	 *
	 * @param ver
	 *            is the version
	 * @param bu
	 *            is the build
	 */
	public void fromStrings(final String ver, final String bu) {
		version = ver;
		build = bu;
	}

	public String full() {
		return version + "-master";
	}

	public String rev() {
		return version;
	}

	public String build() {
		return build;
	}

	/**
	 * This returns a string representation of this object,
	 *
	 * @return a string if this version
	 */
	@Override
	public String toString() {
		return full();
	}

	/**
	 * This calls toString()
	 *
	 * @param csv
	 *            is not used
	 */
	@Override
	public String toString(final boolean csv) {
		return toString();
	}

	/**
	 * This is called by XML parser to decode XML elements
	 *
	 * @see XMLReader#read(InputStream)
	 */
	@Override
	public void xmlElementStart(final String uri, final String tag, final String qname, final Attributes attrs)
			throws SAXException {

		try {
			super.xmlElementStart(uri, tag, qname, attrs);
			return;
		} catch (final SAXException ioe) {
		}

		getLogger().finest("     xmlElementStart() qname=\"" + qname + "\"");

		if (qname.compareToIgnoreCase(getXMLTag()) == 0) {
			fromXml(attrs);
		} else {
			throw new SAXException("invalid qname : " + qname);
		}
	}

	/**
	 * This writes this object to a String as an XML object<br />
	 *
	 * @return a String containing this object definition as XML
	 * @see #fromXml(Attributes)
	 */
	@Override
	public String toXml() {

		String ret = ("<" + getXMLTag() + " ");
		ret += " build=\"" + build + "\"";
		ret += " version=\"" + version + "\"";
		ret += " />";
		return ret;
	}

	/**
	 * This reterives attributes from XML representation<br />
	 *
	 * @param attrs
	 *            contains attributes XML representation
	 * @throws IOException
	 *             on XML error
	 * @see #toXml()
	 */
	@Override
	public void fromXml(final Attributes attrs) {

		if (attrs == null) {
			return;
		}

		final String build = attrs.getValue(1);
		final String version = attrs.getValue(2);
		fromStrings(version, build);
	}

	/**
	 * This writes the current version to stdout
	 *
	 * @since 8.0.1
	 */
	public static void main(final String argv[]) {
		new CommonVersion();
		System.out.println(CommonVersion.getCurrent().full());
	}
}
