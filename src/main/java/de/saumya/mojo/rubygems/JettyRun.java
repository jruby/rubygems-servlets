package de.saumya.mojo.rubygems;

import de.saumya.mojo.mains.JettyRunMain;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


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

        String basedir = props.getProperty( "gem.storage.base", "rubygems" );
        
        setProperty( props, "gem.caching.proxy.storage", basedir + "/caching" );
        setProperty( props, "gem.proxy.storage", basedir + "/proxy" );
        setProperty( props, "gem.hosted.storage", basedir + "/hosted" );
        setProperty( props, "gem.caching.proxy.url", "https://rubygems.org" );
        setProperty( props, "gem.proxy.url", "https://rubygems.org" );

	JettyRunMain.main( props, args );
    }
    
    static private void setProperty( Properties props, String key, String defaultValue ) {
        String result = props.getProperty( key, defaultValue );
        System.setProperty( key, result );
    }
}
