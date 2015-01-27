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
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;

import org.xml.sax.SAXException;

import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;

/**
 * XMLRPCCommandGet.java
 *
 * Created: Nov 16th, 2006
 *
 * @author <a href="mailto:lodygens /a|t\ lal.in2p3.fr>Oleg Lodygensky</a>
 * @since 1.9.0
 */

/**
 * This class defines the XMLRPCCommand to retrieve object definition This does
 * not retrieve Tasks from server
 */
public class XMLRPCCommandGet extends XMLRPCCommand {

	/**
	 * This is the RPC id
	 */
	public static final IdRpc IDRPC = IdRpc.GET;
	/**
	 * This is the XML tag
	 */
	public static final String THISTAG = IDRPC.toString();

	/**
	 * This constructs a new command
	 */
	public XMLRPCCommandGet() throws IOException {
		super(null, IDRPC);
	}

	/**
	 * This constructs a new command
	 */
	protected XMLRPCCommandGet(URI uri, IdRpc id) throws IOException {
		super(uri, id);
	}

	/**
	 * This constructs a new command
	 * 
	 * @param uri
	 *            is the URI of the group to retrieve
	 */
	public XMLRPCCommandGet(URI uri) throws IOException {
		super(uri, IDRPC);
	}

	/**
	 * This constructs a new command
	 * 
	 * @param uri
	 *            is the URI of the group to retrieve
	 */
	public XMLRPCCommandGet(URI uri, UserInterface u) throws IOException {
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
	 * @throws InvalidKeyException
	 * @see xtremweb.common.XMLReader#read(InputStream)
	 */
	public XMLRPCCommandGet(InputStream input) throws IOException,
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
	 * @return an object depending on URI (an app, a data...)
	 * @throws AccessControlException
	 * @throws InvalidKeyException
	 * @exception RemoteException
	 *                is thrown on comm error
	 */
	@Override
	public XMLable exec(CommClient comm) throws IOException,
			ClassNotFoundException, SAXException, InvalidKeyException,
			AccessControlException {
		return comm.get(this);
	}

	/**
	 * This is for testing only. The first argument must be a valid client
	 * configuration file. Without a second argument, this dumps an
	 * XMLRPCCommandGet object. If the second argument is an XML file containing
	 * a description of an XMLRPCCommandGet this creates an object from XML
	 * description and dumps it. <br />
	 * Usage : java -cp xtremweb.jar xtremweb.communications.XMLRPCCommandGet
	 * aConfigFile [anXMLDescriptionFile]
	 */
	public static void main(String[] argv) {
		try {
			final XWConfigurator config = new XWConfigurator(argv[0], false);
			final XMLRPCCommandGet cmd = new XMLRPCCommandGet(new URI(
					config.getCurrentDispatcher(), new UID()), config.getUser());
			cmd.test(argv);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
