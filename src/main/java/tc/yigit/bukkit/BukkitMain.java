package tc.yigit.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import tc.yigit.shared.ConfigFile;
import tc.yigit.shared.SocketConfig;
import tc.yigit.shared.SocketServer;
import tc.yigit.shared.UtilSocket;

public class BukkitMain extends JavaPlugin implements Listener {
	
    private static BukkitMain instance;
    FileConfiguration config;
    private PortlessRelay relay;
    
    public void onEnable() {
        BukkitMain.instance = this;
        SocketConfig.status = "Bukkit";
        
        try (InputStream defaults = this.getResource("config.yml")) {
            ConfigFile.prepare(new File(this.getDataFolder(), "config.yml"), defaults);
        } catch (IOException error) {
            this.getLogger().severe("config.yml hazirlanamadi. Dosya izinlerini ve YAML yazimini kontrol edin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.configLoad();
        
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("websender").setExecutor(new BukkitCommand());
        
        if(SocketConfig.password.equalsIgnoreCase("123qwe")){
            UtilSocket.createLog("PLUGIN DISABLED! PLEASE CHANGE PASSWORD FROM CONFIG!");
            return;
        }

        if ("portless".equalsIgnoreCase(config.getString("connection-mode", "port"))) {
            relay = new PortlessRelay(this);
            relay.start();
            return;
        }
        
        UtilSocket.createLog("Starting...");
        
        // The original SocketServer caches its default port in a static field.
        // Refresh it after loading the actual config for classic TCP mode.
        SocketServer.port = SocketConfig.port;
        SocketServer socket = new SocketServer();
        socket.start();
        
        UtilSocket.createLog("Started!");
    }
    
    public void onDisable(){
        if (relay != null) relay.stop();
        try{
        	if(SocketServer.listenSock != null && !SocketServer.listenSock.isClosed()){
                SocketServer.listenSock.close();        		
        	}
        	if(SocketServer.in != null){
        		SocketServer.in.close();
        	}
        	if(SocketServer.out != null){
        		SocketServer.out.close();
        	}
        	if(SocketServer.sock != null && !SocketServer.sock.isClosed()){
                SocketServer.sock.close();        		
        	}
        }catch(Exception ex){
        	ex.printStackTrace();
        }
    }
    
    public void configLoad() {
        this.reloadConfig();
        this.config = this.getConfig();
        final FileConfiguration config = this.config;
        
        SocketConfig.prefix = String.valueOf(String.valueOf(config.get("senderPrefix")).replaceAll("&", "§")) + " ";
        SocketConfig.port = config.getInt("socketPort");
        SocketConfig.password = config.getString("socketPassword");
        SocketConfig.wrongPassword = config.getString("wrongPassword").replaceAll("&", "§");
        SocketConfig.wrongData = config.getString("wrongData").replaceAll("&", "§");
        SocketConfig.succesfullyLogin = config.getString("succesfullyLogin").replaceAll("&", "§");
        SocketConfig.consoleInfo = config.getString("consoleInfo");
        SocketConfig.commandPermission = config.getString("commandPermission");
        SocketConfig.nothavePerm = config.getString("nothavePerm").replaceAll("&", "§");
        SocketConfig.pluginReloaded = config.getString("pluginReloaded").replaceAll("&", "§");
    }
    
    public static BukkitMain getPlugin(){
        return BukkitMain.instance;
    }
    
    public static boolean sendCommand(String command){
        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
        return true;
    }
    
}
