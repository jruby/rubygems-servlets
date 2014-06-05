package de.saumya.mojo.rubygems;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.ruby.DependencyFile;
import org.sonatype.nexus.ruby.IOUtil;
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
    public void retrieve( RubygemsFile file )
    {
        // find the first and retrieve this
        for( Storage s : storages )
        {
            s.retrieve( file );
            if ( file.exists() )
            {
                return;
            }
            file.resetState();
        }
    }

    private List<InputStream> collectStreams( RubygemsFile file )
    {
        List<InputStream> result = new ArrayList<InputStream>();
        try
        {
            for( Storage s: storages )
            {
                s.retrieve( file );
                if ( file.exists() )
                {
                    result.add( s.getInputStream( file ) );
                }
            }
            file.resetState();
        }
        catch (IOException e)
        {
            file.setException( e );
        }

        return result;
    }
    
    @Override
    public void retrieve( DependencyFile file )
    {
        // merge all existing files
        List<InputStream> streams = collectStreams( file );
        if ( file.hasException() )
        {
            return;
        }
        try
        {
            memory( gateway.mergeDependencies( streams ), file );
        }
        finally
        {
            for( InputStream is : streams )
            {
                IOUtil.close( is );
            }
        }
    }

    @Override
    public void retrieve( SpecsIndexZippedFile file )
    {
        // merge all existing files
        List<InputStream> streams = collectStreams( file );
        if ( file.hasException() )
        {
            return;
        }
        try
        {
            memory( gateway.mergeSpecs( streams, file.specsType() == SpecsIndexType.LATEST ), file );
        }
        finally
        {
            for( InputStream is : streams )
            {
                IOUtil.close( is );
            }
        }
    }    
}