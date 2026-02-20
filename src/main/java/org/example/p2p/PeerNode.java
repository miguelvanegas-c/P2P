package org.example.p2p;

import java.io.*;
import java.net.*;
import java.util.*;
public class PeerNode {
    private final String peerId;
    private final int listenPort;
    private final TrackerClient tracker;
    public PeerNode(String peerId, int listenPort, TrackerClient tracker) {
        this.peerId = peerId;
        this.listenPort = listenPort;
        this.tracker = tracker;
    }
    public void start() throws IOException {
        tracker.register(peerId, listenPort);
        System.out.println("[PEER " + peerId + "] Registered on tracker.");
        new Thread(this::listenLoop).start();
        consoleLoop();
    }
    private void listenLoop() {
        try (ServerSocket ss = new ServerSocket(listenPort)) {
            System.out.println("[PEER " + peerId + "] Listening on " + listenPort);
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handleIncoming(s)).start();
            }
        } catch (IOException e) {
            System.out.println("[PEER " + peerId + "] Listener error: " + e.getMessage());
        }
    }

    private void handleIncoming(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String line = in.readLine();
            if (line == null) return;

            // MSG fromPeerId texto...
            if (line.startsWith("MSG ")) {
                System.out.println("[RECV] " + line.substring(4));
            } else {
                System.out.println("[RECV] " + line);
            }
        } catch (IOException e) {
            System.out.println("[PEER " + peerId + "] Incoming error: " + e.getMessage());
        }
    }

    private void consoleLoop() throws IOException {
        System.out.println("Commands: peers | send <peerId> <msg...> | exit");
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("> ");
            String line = console.readLine();
            if (line == null) return;

            if (line.equalsIgnoreCase("exit")) return;

            if (line.equalsIgnoreCase("peers")) {
                var peers = tracker.listPeers();
                peers.forEach((id, hp) -> System.out.println(id + " -> " + hp.host + ":" + hp.port));
                continue;
            }

            if (line.startsWith("send ")) {
                // send peerB hola mundo
                String[] parts = line.split("\\s+", 3);
                if (parts.length < 3) {
                    System.out.println("Usage: send <peerId> <msg...>");
                    continue;
                }
                String toPeerId = parts[1];
                String msg = parts[2];

                var peers = tracker.listPeers();
                var hp = peers.get(toPeerId);
                if (hp == null) {
                    System.out.println("Unknown peerId: " + toPeerId);
                    continue;
                }
                sendMessage(hp.host, hp.port, msg);
                continue;
            }

            System.out.println("Unknown command.");
        }
    }

    private void sendMessage(String host, int port, String msg) {
        try (Socket s = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            out.write("MSG " + peerId + " " + msg);
            out.newLine();
            out.flush();
            System.out.println("[SENT] to " + host + ":" + port);
        } catch (IOException e) {
            System.out.println("[SEND ERROR] " + e.getMessage());
        }
    }
}