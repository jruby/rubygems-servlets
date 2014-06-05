package de.saumya.mojo.rubygems;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingStorage;
import org.sonatype.nexus.ruby.layout.Storage;

public class LegacyRubygemsServletContextListener extends AbstractRubygemsServletContextListener
{

    static class LegacyRubygemsFileSystem extends NonCachingProxiedRubygemsFileSystem
    {

        public LegacyRubygemsFileSystem( RubygemsGateway gateway, Storage store )
        {
            super( gateway, store );
        }

        @Override
        public RubygemsFile get( String original, String query )
        {
            return super.get( "/maven" + original, query );
        }

        @Override
        public RubygemsFile post( InputStream is, String path )
        {
            throw new RuntimeException( "not supported" );
        }

        @Override
        public RubygemsFile delete( String original )
        {
            throw new RuntimeException( "not supported" );
        }
    }

    public void doContextInitialized( Helper configor ) throws IOException 
    {
        RubygemsGateway gateway = new DefaultRubygemsGateway();
        File path = configor.getFile( "GEM_PROXY_STORAGE" );  
        if ( path == null )
        {
            throw new RuntimeException( "no storage path given");
        }
        Storage storage = new CachingStorage( path, new URL( "https://rubygems.org" ) );
        RubygemsFileSystem rubygems = new LegacyRubygemsFileSystem( gateway, storage );
        configor.register( RubygemsFileSystem.class.getName(), rubygems );
    }

}
