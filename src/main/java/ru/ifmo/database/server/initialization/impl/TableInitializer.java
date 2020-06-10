package ru.ifmo.database.server.initialization.impl;

import ru.ifmo.database.server.exception.DatabaseException;
import ru.ifmo.database.server.index.impl.SegmentIndex;
import ru.ifmo.database.server.initialization.InitializationContext;
import ru.ifmo.database.server.initialization.Initializer;
import ru.ifmo.database.server.logic.Table;
import ru.ifmo.database.server.logic.impl.CachingTable;
import ru.ifmo.database.server.logic.impl.TableImpl;

import java.io.File;
import java.util.ArrayList;

public class TableInitializer implements Initializer {

    private final Initializer segmentInitializer;

    public TableInitializer(Initializer segmentInitializer) {
        this.segmentInitializer = segmentInitializer;
    }

    @Override
    public void perform(InitializationContext context) throws DatabaseException {
        if (context.currentTableContext() == null) {
            throw new DatabaseException("Segment context  equals zero");
        }
        File dir = new File(context.currentTableContext().getTablePath().toString());
        if (dir.listFiles() == null) {
            return;
        }
        ArrayList<InitializationContext> contexts = context.currentTableContext().getInitializationContexts();
        //noinspection ConstantConditions
        for (File segment :
                dir.listFiles((dir1, name) -> name.startsWith(context.currentTableContext().getTableName()))) {

            InitializationContext initializationContext = InitializationContextImpl.builder()
                    .executionEnvironment(context.executionEnvironment())
                    .currentDatabaseContext(context.currentDbContext())
                    .currentTableContext(context.currentTableContext())
                    .currentSegmentContext(new SegmentInitializationContextImpl(segment.getName(),
                            segment.toPath(),
                            new SegmentIndex(),
                            segment.length()))
                    .build();
            contexts.add(initializationContext);
            segmentInitializer.perform(initializationContext);
        }
        Table cachingTable = new CachingTable(TableImpl.initializeFromContext(context.currentTableContext()));
        context.currentDbContext().addTable(cachingTable);

    }
}
