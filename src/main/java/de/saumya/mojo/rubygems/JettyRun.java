package de.saumya.mojo.rubygems;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Properties;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyRun
{

    public static void main(String[] args)  {
        Properties props = new Properties();
        String filename = args.length == 0 ? "rubygems.properties" : args[ 0 ];
        System.err.println();
        try
        {
            props.load( new FileReader( filename ) );
            System.err.println( "loaded properties from " + filename );
        }
        catch (IOException e)
        {
            System.err.println( "could not load properties from " + filename + " :" + e.getMessage() );
            System.err.println( "      using defaults" );
    	}
        System.err.println();

        Server server = new Server();
        SocketConnector connector = new SocketConnector();
     
        // Set some timeout options to make debugging easier.
        connector.setMaxIdleTime(1000 * 60 * 60);
        connector.setSoLingerTime(-1);
        connector.setPort(Integer.parseInt( props.getProperty( "port", "8989" ) ) );
        connector.setHost( props.getProperty( "host" ) );
        server.setConnectors(new Connector[] { connector });

        String basedir = props.getProperty( "gem.storage.base", "rubygems" );
        
        setProperty( props, "gem.caching.proxy.storage", basedir + "/caching" );
        setProperty( props, "gem.proxy.storage", basedir + "/proxy" );
        setProperty( props, "gem.hosted.storage", basedir + "/hosted" );
        setProperty( props, "gem.caching.proxy.url", "https://rubygems.org" );
        setProperty( props, "gem.proxy.url", "https://rubygems.org" );

        WebAppContext context = new WebAppContext();
        context.setServer(server);
        context.setContextPath("/");
        context.setExtractWAR( false );
        context.setCopyWebInf( true );
     
        ProtectionDomain protectionDomain = JettyRun.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        context.setWar(location.toExternalForm());
     
        server.setHandler(context);
	Runtime.getRuntime().addShutdownHook(new JettyStop(server));
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(100);
        }
    }
    
    static private void setProperty( Properties props, String key, String defaultValue ) {
        String result = props.getProperty( key, defaultValue );
        System.setProperty( key, result );
    }
}
