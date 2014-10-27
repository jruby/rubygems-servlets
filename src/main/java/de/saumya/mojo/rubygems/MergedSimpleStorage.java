package de.saumya.mojo.rubygems;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.DependencyHelper;
import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.MergeSpecsHelper;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.SpecsIndexType;
import org.sonatype.nexus.ruby.SpecsIndexZippedFile;
import org.sonatype.nexus.ruby.layout.SimpleStorage;
import org.sonatype.nexus.ruby.layout.Storage;

public class MergedSimpleStorage extends SimpleStorage
{

    private final List<Storage> storages;
    private final RubygemsGateway gateway;

    MergedSimpleStorage( RubygemsGateway gateway, List<Storage> storages )
    {
        super( null ); // can not create/retrieve/update/delete files
        this.gateway = gateway;
        this.storages = storages;
    }

    @Override
    public InputStream getInputStream( RubygemsFile file ) throws IOException
    {
        if ( file.hasException() )
        {
            throw new IOException( file.getException() );
        }
        if ( file.get() == null ) {
            for( Storage s : storages )
            {
                s.retrieve( file );
                if ( file.exists() )
                {
                    return s.getInputStream( file );
                }
                file.resetState();
            }
        }
        return super.getInputStream( file );
    }

    @Override
    public String[] listDirectory( Directory dir )
    {
        // TODO 
        return new String[ 0 ];
    }

    @Override
    public void retrieve( RubygemsFile file )
    {
        // find the first and retrieve this
        for( Storage s : storages )
        {
            file.resetState();
            s.retrieve( file );
            if ( file.exists() )
            {
                return;
            }
        }
    }
    
    @Override
    public void retrieve( DependencyFile file )
    {
        DependencyHelper deps = gateway.newDependencyHelper();
        try {
            // merge all existing files
            for( Storage s: storages )
            {
                s.retrieve( file );
                if ( file.exists() )
                {
                    try ( InputStream is = s.getInputStream( file ) ) {
                        deps.add( is );
                    }
                }
                file.resetState();
            }            
            file.resetState();
            
            // no need to close the stream since it is a ByteArrayInputStream
            memory( deps.getInputStream( false ), file );
        }
        catch( IOException e ) {
            file.setException( e );
        }
    }

    @Override
    public void retrieve( SpecsIndexZippedFile file )
    {
        MergeSpecsHelper merge = gateway.newMergeSpecsHelper();
        
        try {
            // merge all existing files
            for( Storage s: storages )
            {
                s.retrieve( file );
                if ( file.exists() )
                {
                    try ( InputStream is = new GZIPInputStream( s.getInputStream( file ) ) )
                    {
                        merge.add( is );
                    }
                }
                file.resetState();
            }
            
            memory( (ByteArrayInputStream) IOUtil.toGzipped( merge.getInputStream( file.specsType() == SpecsIndexType.LATEST ) ), file );
        }
        catch( IOException e )
        {
            file.setException( e );
        }
    }    
}