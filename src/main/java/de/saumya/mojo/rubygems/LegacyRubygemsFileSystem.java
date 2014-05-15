package de.saumya.mojo.rubygems;

import java.io.InputStream;

import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.Storage;

public class LegacyRubygemsFileSystem extends ProxiedRubygemsFileSystem
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
