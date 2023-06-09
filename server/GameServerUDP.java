package server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import tage.Log;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {

	private NPCController npcController;
	private UUID npcClient;
	private int blockHandlerUpdates;
	private HashMap<UUID, Boolean> readyStatus;
	private ArrayList<String> finished;
	private int checkpoint = 0;
	private int position = 0;

	public GameServerUDP(int localPort, NPCController npcController) throws IOException
	{
		super(localPort, ProtocolType.UDP);
		this.npcController = npcController;
		blockHandlerUpdates = 0;
		readyStatus = new HashMap<>();
		finished = new ArrayList<>();
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		String message = (String) o;
		String[] messageTokens = message.split(",");

		if (messageTokens.length > 0)
		{
			if (messageTokens[0].compareTo("join") == 0)
			{
				try
				{
					IClientInfo ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);
					addClient(ci, clientID);
					System.out.println("Join request received from - " + clientID.toString());
					sendJoinedMessage(clientID, true);
					readyStatus.put(clientID, false);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			else if (messageTokens[0].compareTo("bye") == 0)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("Exit request received from - " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
			}

			else if (messageTokens[0].compareTo("create") == 0)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendCreateMessages(clientID,
						message.substring(messageTokens[0].length() + messageTokens[1].length() + 2));
				sendWantsDetailsMessages(clientID);
			}

			else if (messageTokens[0].compareTo("dsfr") == 0)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				sendDetailsForMessage(clientID, remoteID, message.substring(
						messageTokens[0].length() + messageTokens[1].length() + messageTokens[2].length() + 3));
			}

			else if (messageTokens[0].compareTo("move") == 0)
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendMoveMessages(clientID,
						message.substring(messageTokens[0].length() + messageTokens[1].length() + 2));
			}

			else if (messageTokens[0].compareTo("npcmove") == 0)
			{
				if (blockHandlerUpdates <= 0)
				{
					UUID clientID = UUID.fromString(messageTokens[1]);
					String msg = message.substring(messageTokens[0].length() + messageTokens[1].length() + 2);
					sendNPCmove(clientID, msg);
					npcController.updateNpc(msg);
				} else
				{
					blockHandlerUpdates--;
				}
			}

			else if (messageTokens[0].compareTo("getnpc") == 0)
			{
				Log.trace("getnpc message received\n");
				if (npcClient == null)
				{
					Log.trace("npcClient is null\n");
					npcController.init(this);
					npcClient = UUID.fromString(messageTokens[1]);
					Log.trace("Asking client for targets\n");
					sendGetNPCTargets(npcClient);
					Log.trace("Sending createNPC message\n");
					sendCreateNPCmsg(npcClient, npcController.getNpcStatus());
				} else
				{
					Log.trace("Sending createNPC message\n");
					sendCreateNPCmsg(UUID.fromString(messageTokens[1]), npcController.getNpcStatus());
				}
			}

			else if (messageTokens[0].compareTo("npctargets") == 0)
			{
				Log.trace("npctargets message received\n");
				npcController.setTargets(message.substring(messageTokens[0].length() + 1));
			}

			else if (messageTokens[0].compareTo("forcenpcmove") == 0)
			{
				blockHandlerUpdates = 5;
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendNPCmove(clientID, message.substring(messageTokens[0].length() + messageTokens[1].length() + 2));
				npcController.updateNpc(message.substring(messageTokens[0].length() + messageTokens[1].length() + 2));
			}

			else if (messageTokens[0].compareTo("preprace") == 0)
			{
				readyStatus.put(UUID.fromString(messageTokens[1]), true);
				checkRaceStart();
			}

			else if (messageTokens[0].compareTo("finished") == 0)
			{
				finished.add(messageTokens[1]);

				sendPosition(UUID.fromString(messageTokens[1]), finished.size());
			}

			else if (messageTokens[0].compareTo("checkpoint") == 0)
			{
				updateCheckpoint(messageTokens[1], Integer.parseInt(messageTokens[2]));
			}

			else
			{
				Log.print("Unhandled message: %s\n", message);
			}
		}
	}

	public void updateCheckpoint(String uuid, int checkpoint)
	{
		if (checkpoint > this.checkpoint)
		{
			this.checkpoint = checkpoint;
			position = 0;
		}

		if (!uuid.equals("npc"))
		{
			sendPosition(UUID.fromString(uuid), ++position);
		} 
		else
		{
			position++;
		}
	}

	private void checkRaceStart()
	{
		boolean allReady = true;
		for (Boolean b : readyStatus.values())
		{
			if (!b)
			{
				allReady = false;
				break;
			}
		}

		if (allReady)
		{
			sendGoToRaceStart();
			Log.print("Starting race sequence!\n");
			int wait = 5;
			Log.print("Waiting for %d seconds...\n", wait);
			try
			{
				Thread.sleep(wait * 1000);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			Log.print("Start race!\n");
			sendRaceStart();
		}
	}

	private void sendRaceStart()
	{
		try
		{
			String message = new String("racestart");
			sendPacketToAll(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void sendGoToRaceStart()
	{
		try
		{
			String message = new String("gotoracestart,");
			int i = 0;
			for (HashMap.Entry<UUID, Boolean> set : readyStatus.entrySet())
			{
				sendPacket(message + i, set.getKey());
				i++;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void sendPosition(UUID clientID, int position)
	{
		try
		{
			String message = new String("position," + position);
			sendPacket(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs the client who just requested to join the server if their request was
	 * able to be granted.
	 * 
	 * @param clientID The client that sent the JOIN message
	 * @param success  Whether or not the client was able to join the server
	 */
	public void sendJoinedMessage(UUID clientID, boolean success)
	{
		try
		{
			System.out.println("trying to confirm join");
			String message = new String("join,");
			if (success)
				message += "success";
			else
				message += "failure";
			sendPacket(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs a client that the avatar with the identifier remoteId has left the
	 * server. This message is meant to be sent to all clients currently connected
	 * to the server when a client leaves the server.
	 * 
	 * @param clientID The client that sent the BYE message
	 */
	public void sendByeMessages(UUID clientID)
	{
		try
		{
			String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs a client that a new avatar has joined the server with the unique
	 * identifier remoteId. This message is intended to be send to all clients
	 * currently connected to the server when a new client has joined the server and
	 * sent a create message to the server. This message also triggers WANTS_DETAILS
	 * messages to be sent to all clients connected to the server.
	 * 
	 * @param clientID The client that sent the CREATE message
	 * @param data     The data from the CREATE message
	 */
	public void sendCreateMessages(UUID clientID, String data)
	{
		try
		{
			String message = new String("create," + clientID.toString());
			message += "," + data;
			forwardPacketToAll(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs a client of the details for a remote client's avatar. This message is
	 * in response to the server receiving a DETAILS_FOR message from a remote
	 * client. That remote client's message's localId becomes the remoteId for this
	 * message, and the remote client's message's remoteId is used to send this
	 * message to the proper client.
	 * 
	 * @param clientID The client that sent the DETAILS_FOR message
	 * @param remoteId The client that the DETAILS_FOR message is meant for
	 * @param data     The data from the DSFR message
	 */
	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String data)
	{
		try
		{
			String message = new String("dsfr," + remoteId.toString());
			message += "," + data;

			sendPacket(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs a local client that a remote client wants the local client's avatar's
	 * information. This message is meant to be sent to all clients connected to the
	 * server when a new client joins the server.
	 * 
	 * @param clientID The client that sent the JOIN message
	 */
	public void sendWantsDetailsMessages(UUID clientID)
	{
		try
		{
			String message = new String("wsds," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs a client that a remote client's avatar has changed position. This
	 * message is meant to be forwarded to all clients connected to the server when
	 * it receives a MOVE message from the remote client.
	 * 
	 * @param clientID The client that sent the MOVE message
	 * @param data     The data from the MOVE message
	 */
	public void sendMoveMessages(UUID clientID, String data)
	{
		try
		{
			String message = new String("move," + clientID.toString());
			message += "," + data;
			forwardPacketToAll(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Informs client to create an NPC ghost avatar.
	 * 
	 * @param clientID The client that sent the CREATENPC message
	 * @param data     The data from the CNPC message
	 */
	public void sendCreateNPCmsg(UUID clientID, String data)
	{
		try
		{
			String message = new String("createnpc");
			message += "," + data;
			sendPacket(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendGetNPCTargets(UUID clientID)
	{
		try
		{
			String message = new String("getnpctargets");
			sendPacket(message, npcClient);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendNPCStatus(String data)
	{
		try
		{
			String message = new String("npcstatus," + data);
			sendPacket(message, npcClient);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendNPCmove(UUID clientID, String data)
	{
		try
		{
			String message = new String("npcmove," + data);
			forwardPacketToAll(message, clientID);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void npcFinished()
	{
		finished.add("npc");
		Log.print("NPC finished! %d\n", finished.size());
	}
}
