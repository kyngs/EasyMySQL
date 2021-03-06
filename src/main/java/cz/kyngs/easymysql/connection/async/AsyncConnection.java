/*
 * Copyright (c) 2020 kyngs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 */

package cz.kyngs.easymysql.connection.async;

import com.zaxxer.hikari.HikariDataSource;
import cz.kyngs.easymysql.connection.AbstractConnection;
import cz.kyngs.easymysql.utils.ThrowableConsumer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncConnection extends AbstractConnection {

    private final BlockingQueue<ThrowableConsumer<Connection, Exception>> connectionQueue;
    private final AsyncThreadWorker[] workers;

    public AsyncConnection(HikariDataSource dataSource, int threadCount) throws SQLException {
        super(dataSource);
        connectionQueue = new LinkedBlockingQueue<>();
        workers = new AsyncThreadWorker[threadCount];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new AsyncThreadWorker(this, i);
        }
    }

    @Override
    public void schedule(ThrowableConsumer<Connection, Exception> task) {
        connectionQueue.add(task);
    }

    protected ThrowableConsumer<Connection, Exception> getWork() throws InterruptedException {
        return connectionQueue.take();
    }

    public void close() {
        for (AsyncThreadWorker worker : workers) {
            worker.stop();
        }
    }

}
