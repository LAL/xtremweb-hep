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

package xtremweb.communications;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.SAXException;

import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

/**
 * XMLRPCCommandGetSessionWorks.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand to retreive session jobs
 */
public class XMLRPCCommandGetSessionWorks extends XMLRPCCommand {

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.GETSESSIONWORKS;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This constructs a new command
	 */
	protected XMLRPCCommandGetSessionWorks() throws IOException {
		super(null, IDRPC);
	}

	/**
	 * This constructs a new command
	 * 
	 * @param uri
	 *            contains the URI to connect to; its path must contains the UID
	 *            of the session to retrieve works for
	 */
	protected XMLRPCCommandGetSessionWorks(URI uri) throws IOException {
		super(uri, IDRPC);
	}

	/**
	 * This constructs a new command
	 * 
	 * @param uri
	 *            contains the URI to connect to; its path must contains the UID
	 *            of the session to retrieve works for
	 * @param u
	 *            defines the user who executes this command
	 */
	public XMLRPCCommandGetSessionWorks(URI uri, UserInterface u)
			throws IOException {
		this(uri);
		setUser(u);
	}

	/**
	 * This constructs a new object from XML attributes received from input
	 * stream
	 * 
	 * @param input
	 *            is the input stream
	 * @throws IOException
	 *             on XML error
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	public XMLRPCCommandGetSessionWorks(InputStream input) throws IOException,
			SAXException, InvalidKeyException {
		this();
		final XMLReader reader = new XMLReader(this);
		reader.read(input);
	}

	/**
	 * This sends this command to server and returns answer
	 * 
	 * @param comm
	 *            is the communication channel
	 * @return always null since this expect no answer
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 */
	@Override
	public XMLable exec(CommClient comm) throws IOException, SAXException,
			InvalidKeyException, AccessControlException {
		return comm.getSessionWorks(this);
	}

	/**
	 * This is for testing only. The first argument must be a valid client
	 * configuration file. Without a second argument, this dumps an
	 * XMLRPCCommandGetSessionWorks object. If the second argument is an XML
	 * file containing a description of an XMLRPCCommandGetSessionWorks this
	 * creates an object from XML description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar
	 * xtremweb.communications.XMLRPCCommandGetSessionWorks aConfigFile
	 * [anXMLDescriptionFile]
	 */
	public static void main(String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			final XMLRPCCommandGetSessionWorks cmd = new XMLRPCCommandGetSessionWorks(
					new URI(config.getCurrentDispatcher(), new UID()),
					config.getUser());
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}