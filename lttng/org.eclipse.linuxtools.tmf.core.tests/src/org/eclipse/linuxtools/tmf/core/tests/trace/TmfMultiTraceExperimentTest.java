/*******************************************************************************
 * Copyright (c) 2009, 2010, 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Adjusted for new Trace Model
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.trace;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.internal.tmf.core.trace.TmfExperimentContext;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.request.TmfDataRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.tests.TmfCoreTestPlugin;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfExperimentStub;
import org.eclipse.linuxtools.tmf.tests.stubs.trace.TmfTraceStub;

/**
 * Test suite for the TmfExperiment class (multiple traces).
 */
@SuppressWarnings("nls")
public class TmfMultiTraceExperimentTest extends TestCase {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final String DIRECTORY    = "testfiles";
    private static final String TEST_STREAM1 = "O-Test-10K";
    private static final String TEST_STREAM2 = "E-Test-10K";
    private static final String EXPERIMENT   = "MyExperiment";
    private static int          NB_EVENTS    = 20000;
    private static int          BLOCK_SIZE   = 1000;

    private static ITmfTrace<TmfEvent>[] fTraces;
    private static TmfExperimentStub<TmfEvent> fExperiment;

    private static byte SCALE = (byte) -3;

    // ------------------------------------------------------------------------
    // Housekeeping
    // ------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private synchronized static ITmfTrace<?>[] setupTrace(final String path1, final String path2) {
        if (fTraces == null) {
            fTraces = new ITmfTrace[2];
            try {
                URL location = FileLocator.find(TmfCoreTestPlugin.getDefault().getBundle(), new Path(path1), null);
                File test = new File(FileLocator.toFileURL(location).toURI());
                final TmfTraceStub trace1 = new TmfTraceStub(test.getPath(), 0, true);
                fTraces[0] = trace1;
                location = FileLocator.find(TmfCoreTestPlugin.getDefault().getBundle(), new Path(path2), null);
                test = new File(FileLocator.toFileURL(location).toURI());
                final TmfTraceStub trace2 = new TmfTraceStub(test.getPath(), 0, true);
                fTraces[1] = trace2;
            } catch (final TmfTraceException e) {
                e.printStackTrace();
            } catch (final URISyntaxException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return fTraces;
    }

    public TmfMultiTraceExperimentTest(final String name) throws Exception {
        super(name);
    }

    @Override
    protected synchronized void setUp() throws Exception {
        super.setUp();
        setupTrace(DIRECTORY + File.separator + TEST_STREAM1, DIRECTORY + File.separator + TEST_STREAM2);
        if (fExperiment == null) {
            fExperiment = new TmfExperimentStub<TmfEvent>(EXPERIMENT, fTraces, BLOCK_SIZE);
            fExperiment.getIndexer().buildIndex(0, TmfTimeRange.ETERNITY, true);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public void testBasicTmfExperimentConstructor() {

        assertEquals("GetId", EXPERIMENT, fExperiment.getName());
        assertEquals("GetNbEvents", NB_EVENTS, fExperiment.getNbEvents());

        final TmfTimeRange timeRange = fExperiment.getTimeRange();
        assertEquals("getStartTime", 1, timeRange.getStartTime().getValue());
        assertEquals("getEndTime", NB_EVENTS, timeRange.getEndTime().getValue());
    }

    // ------------------------------------------------------------------------
    // seekEvent on rank
    // ------------------------------------------------------------------------

    public void testSeekRankOnCacheBoundary() throws Exception {

        long cacheSize = fExperiment.getCacheSize();

        // On lower bound, returns the first event (TS = 1)
        ITmfContext context = fExperiment.seekEvent(0);
        assertEquals("Context rank", 0, context.getRank());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1, event.getTimestamp().getValue());
        assertEquals("Context rank", 1, context.getRank());

        // Position trace at event rank [cacheSize]
        context = fExperiment.seekEvent(cacheSize);
        assertEquals("Context rank", cacheSize, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize + 1, event.getTimestamp().getValue());
        assertEquals("Context rank", cacheSize + 1, context.getRank());

        // Position trace at event rank [4 * cacheSize]
        context = fExperiment.seekEvent(4 * cacheSize);
        assertEquals("Context rank", 4 * cacheSize, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4 * cacheSize + 1, event.getTimestamp().getValue());
        assertEquals("Context rank", 4 * cacheSize + 1, context.getRank());
    }

    public void testSeekRankNotOnCacheBoundary() throws Exception {

        long cacheSize = fExperiment.getCacheSize();

        // Position trace at event rank 9
        ITmfContext context = fExperiment.seekEvent(9);
        assertEquals("Context rank", 9, context.getRank());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 10, event.getTimestamp().getValue());
        assertEquals("Context rank", 10, context.getRank());

        // Position trace at event rank [cacheSize - 1]
        context = fExperiment.seekEvent(cacheSize - 1);
        assertEquals("Context rank", cacheSize - 1, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize, event.getTimestamp().getValue());
        assertEquals("Context rank", cacheSize, context.getRank());

        // Position trace at event rank [cacheSize + 1]
        context = fExperiment.seekEvent(cacheSize + 1);
        assertEquals("Context rank", cacheSize + 1, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize + 2, event.getTimestamp().getValue());
        assertEquals("Context rank", cacheSize + 2, context.getRank());

        // Position trace at event rank 4500
        context = fExperiment.seekEvent(4500);
        assertEquals("Context rank", 4500, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4501, event.getTimestamp().getValue());
        assertEquals("Context rank", 4501, context.getRank());
    }

    public void testSeekRankOutOfScope() throws Exception {

        // Position trace at beginning
        ITmfContext context = fExperiment.seekEvent(-1);
        assertEquals("Event rank", 0, context.getRank());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1, event.getTimestamp().getValue());
        assertEquals("Context rank", 1, context.getRank());

        // Position trace at event passed the end
        context = fExperiment.seekEvent(NB_EVENTS);
        assertEquals("Context rank", NB_EVENTS, context.getRank());

        event = fExperiment.getNext(context);
        assertNull("Event", event);
        assertEquals("Context rank", NB_EVENTS, context.getRank());
    }

    // ------------------------------------------------------------------------
    // seekEvent on timestamp
    // ------------------------------------------------------------------------

    public void testSeekTimestampOnCacheBoundary() throws Exception {

        long cacheSize = fExperiment.getCacheSize();

        // Position trace at event rank 0
        ITmfContext context = fExperiment.seekEvent(new TmfTimestamp(1, SCALE, 0));
        assertEquals("Context rank", 0, context.getRank());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1, event.getTimestamp().getValue());
        assertEquals("Context rank", 1, context.getRank());

        // Position trace at event rank [cacheSize]
        context = fExperiment.seekEvent(new TmfTimestamp(cacheSize + 1, SCALE, 0));
        assertEquals("Event rank", cacheSize, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize + 1, event.getTimestamp().getValue());
        assertEquals("Context rank", cacheSize + 1, context.getRank());

        // Position trace at event rank [4 * cacheSize]
        context = fExperiment.seekEvent(new TmfTimestamp(4 * cacheSize + 1, SCALE, 0));
        assertEquals("Context rank", 4 * cacheSize, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4 * cacheSize + 1, event.getTimestamp().getValue());
        assertEquals("Context rank", 4 * cacheSize + 1, context.getRank());
    }

    public void testSeekTimestampNotOnCacheBoundary() throws Exception {

        // Position trace at event rank 1 (TS = 2)
        ITmfContext context = fExperiment.seekEvent(new TmfTimestamp(2, SCALE, 0));
        assertEquals("Context rank", 1, context.getRank());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 2, event.getTimestamp().getValue());
        assertEquals("Context rank", 2, context.getRank());

        // Position trace at event rank 9 (TS = 10)
        context = fExperiment.seekEvent(new TmfTimestamp(10, SCALE, 0));
        assertEquals("Context rank", 9, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 10, event.getTimestamp().getValue());
        assertEquals("Context rank", 10, context.getRank());

        // Position trace at event rank 999 (TS = 1000)
        context = fExperiment.seekEvent(new TmfTimestamp(1000, SCALE, 0));
        assertEquals("Context rank", 999, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1000, event.getTimestamp().getValue());
        assertEquals("Context rank", 1000, context.getRank());

        // Position trace at event rank 1001 (TS = 1002)
        context = fExperiment.seekEvent(new TmfTimestamp(1002, SCALE, 0));
        assertEquals("Context rank", 1001, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1002, event.getTimestamp().getValue());
        assertEquals("Context rank", 1002, context.getRank());

        // Position trace at event rank 4500 (TS = 4501)
        context = fExperiment.seekEvent(new TmfTimestamp(4501, SCALE, 0));
        assertEquals("Context rank", 4500, context.getRank());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4501, event.getTimestamp().getValue());
        assertEquals("Context rank", 4501, context.getRank());
    }

    public void testSeekTimestampOutOfScope() throws Exception {

        // Position trace at beginning
        ITmfContext context = fExperiment.seekEvent(new TmfTimestamp(-1, SCALE, 0));
        assertEquals("Event rank", 0, context.getRank());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1, event.getTimestamp().getValue());
        assertEquals("Event rank", 1, context.getRank());

        // Position trace at event passed the end
        context = fExperiment.seekEvent(new TmfTimestamp(NB_EVENTS + 1, SCALE, 0));
        event = fExperiment.getNext(context);
        assertNull("Event location", event);
        assertEquals("Event rank", ITmfContext.UNKNOWN_RANK, context.getRank());
    }

    // ------------------------------------------------------------------------
    // seekEvent by location (context rank is undefined)
    // ------------------------------------------------------------------------

    public void testSeekLocationOnCacheBoundary() throws Exception {

        long cacheSize = fExperiment.getCacheSize();

        // Position trace at event rank 0
        ITmfContext tmpContext = fExperiment.seekEvent(0);
        ITmfContext context = fExperiment.seekEvent(tmpContext.getLocation());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1, event.getTimestamp().getValue());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 2, event.getTimestamp().getValue());

        // Position trace at event rank 'cacheSize'
        tmpContext = fExperiment.seekEvent(cacheSize);
        context = fExperiment.seekEvent(tmpContext.getLocation());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize + 1, event.getTimestamp().getValue());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize + 2, event.getTimestamp().getValue());

        // Position trace at event rank 4 * 'cacheSize'
        tmpContext = fExperiment.seekEvent(4 * cacheSize);
        context = fExperiment.seekEvent(tmpContext.getLocation());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4 * cacheSize + 1, event.getTimestamp().getValue());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4 * cacheSize + 2, event.getTimestamp().getValue());
    }

    public void testSeekLocationNotOnCacheBoundary() throws Exception {

        long cacheSize = fExperiment.getCacheSize();

        // Position trace at event 'cacheSize' - 1
        ITmfContext tmpContext = fExperiment.seekEvent(cacheSize - 1);
        ITmfContext context = fExperiment.seekEvent(tmpContext.getLocation());

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize, event.getTimestamp().getValue());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", cacheSize + 1, event.getTimestamp().getValue());

        // Position trace at event rank 2 * 'cacheSize' - 1
        tmpContext = fExperiment.seekEvent(2 * cacheSize - 1);
        context = fExperiment.seekEvent(tmpContext.getLocation());
        context = fExperiment.seekEvent(2 * cacheSize - 1);

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 2 * cacheSize, event.getTimestamp().getValue());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 2 * cacheSize + 1, event.getTimestamp().getValue());

        // Position trace at event rank 4500
        tmpContext = fExperiment.seekEvent(4500);
        context = fExperiment.seekEvent(tmpContext.getLocation());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4501, event.getTimestamp().getValue());

        event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 4502, event.getTimestamp().getValue());
    }

    public void testSeekLocationOutOfScope() throws Exception {

        // Position trace at beginning
        ITmfContext context = fExperiment.seekEvent((ITmfLocation<?>) null);

        ITmfEvent event = fExperiment.getNext(context);
        assertEquals("Event timestamp", 1, event.getTimestamp().getValue());
    }

    // ------------------------------------------------------------------------
    // getNext - updates the context
    // ------------------------------------------------------------------------

    private void validateContextRanks(ITmfContext context) {
        assertTrue("Experiment context type", context instanceof TmfExperimentContext);
        TmfExperimentContext ctx = (TmfExperimentContext) context;

        int nbTraces = ctx.getContexts().length;

        // expRank = sum(trace ranks) - nbTraces + 1 (if lastTraceRead != NO_TRACE)
        long expRank = -nbTraces + ((ctx.getLastTrace() != TmfExperimentContext.NO_TRACE) ? 1 : 0);
        for (int i = 0; i < nbTraces; i++) {
            long rank = ctx.getContexts()[i].getRank();
            if (rank == -1) {
                expRank = -1;
                break;
            }
            expRank += rank;
        }
        assertEquals("Experiment context rank", expRank, ctx.getRank());
    }

    public void testGetNextAfteSeekingOnTS_1() throws Exception {

        final long INITIAL_TS = 1;
        final int NB_READS = 20;

        // On lower bound, returns the first event (ts = 1)
        final ITmfContext context = fExperiment.seekEvent(new TmfTimestamp(INITIAL_TS, SCALE, 0));

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_TS + i, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_TS + i, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_TS + NB_READS, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_TS + NB_READS - 1, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfteSeekingOnTS_2() throws Exception {

        final long INITIAL_TS = 2;
        final int NB_READS = 20;

        // On lower bound, returns the first event (ts = 2)
        final ITmfContext context = fExperiment.seekEvent(new TmfTimestamp(INITIAL_TS, SCALE, 0));

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_TS + i, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_TS + i, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_TS + NB_READS, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_TS + NB_READS - 1, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfteSeekingOnTS_3() throws Exception {

        final long INITIAL_TS = 500;
        final int NB_READS = 20;

        // On lower bound, returns the first event (ts = 500)
        final ITmfContext context = fExperiment.seekEvent(new TmfTimestamp(INITIAL_TS, SCALE, 0));

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_TS + i, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_TS + i, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_TS + NB_READS, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_TS + NB_READS - 1, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfterSeekingOnRank_1() throws Exception {

        final long INITIAL_RANK = 0L;
        final int NB_READS = 20;

        // On lower bound, returns the first event (rank = 0)
        final ITmfContext context = fExperiment.seekEvent(INITIAL_RANK);

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_RANK + i + 1, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_RANK + i + 1, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_RANK + NB_READS + 1, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_RANK + NB_READS, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfterSeekingOnRank_2() throws Exception {

        final long INITIAL_RANK = 1L;
        final int NB_READS = 20;

        // On lower bound, returns the first event (rank = 0)
        final ITmfContext context = fExperiment.seekEvent(INITIAL_RANK);

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_RANK + i + 1, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_RANK + i + 1, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_RANK + NB_READS + 1, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_RANK + NB_READS, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfterSeekingOnRank_3() throws Exception {

        final long INITIAL_RANK = 500L;
        final int NB_READS = 20;

        // On lower bound, returns the first event (rank = 0)
        final ITmfContext context = fExperiment.seekEvent(INITIAL_RANK);

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_RANK + i + 1, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_RANK + i + 1, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_RANK + NB_READS + 1, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_RANK + NB_READS, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfterSeekingOnLocation_1() throws Exception {

        final ITmfLocation<?> INITIAL_LOC = null;
        final long INITIAL_TS = 1;
        final int NB_READS = 20;

        // On lower bound, returns the first event (ts = 1)
        final ITmfContext context = fExperiment.seekEvent(INITIAL_LOC);

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_TS + i, event.getTimestamp().getValue());
            assertEquals("Event rank", INITIAL_TS + i, context.getRank());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_TS + NB_READS, event.getTimestamp().getValue());
        assertEquals("Event rank", INITIAL_TS + NB_READS - 1, context.getRank());

        validateContextRanks(context);
    }

    public void testGetNextAfterSeekingOnLocation_2() throws Exception {

        final ITmfLocation<?> INITIAL_LOC = fExperiment.seekEvent(1L).getLocation();
        final long INITIAL_TS = 2;
        final int NB_READS = 20;

        // On lower bound, returns the first event (ts = 2)
        final ITmfContext context = fExperiment.seekEvent(INITIAL_LOC);

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_TS + i, event.getTimestamp().getValue());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_TS + NB_READS, event.getTimestamp().getValue());

        validateContextRanks(context);
    }

    public void testGetNextAfterSeekingOnLocation_3() throws Exception {

        final ITmfLocation<?> INITIAL_LOC = fExperiment.seekEvent(500L).getLocation();
        final long INITIAL_TS = 501;
        final int NB_READS = 20;

        // On lower bound, returns the first event (ts = 501)
        final ITmfContext context = fExperiment.seekEvent(INITIAL_LOC);

        validateContextRanks(context);

        // Read NB_EVENTS
        ITmfEvent event;
        for (int i = 0; i < NB_READS; i++) {
            event = fExperiment.getNext(context);
            assertEquals("Event timestamp", INITIAL_TS + i, event.getTimestamp().getValue());
        }

        // Make sure we stay positioned
        event = fExperiment.parseEvent(context);
        assertEquals("Event timestamp", INITIAL_TS + NB_READS, event.getTimestamp().getValue());

        validateContextRanks(context);
    }

    public void testGetNextLocation() throws Exception {
        ITmfContext context1 = fExperiment.seekEvent(0);
        fExperiment.getNext(context1);
        ITmfLocation<?> location = context1.getLocation().clone();
        ITmfEvent event1 = fExperiment.getNext(context1);
        ITmfContext context2 = fExperiment.seekEvent(location);
        ITmfEvent event2 = fExperiment.getNext(context2);
        assertEquals("Event timestamp", event1.getTimestamp().getValue(), event2.getTimestamp().getValue());
    }

    public void testGetNextEndLocation() throws Exception {
        ITmfContext context1 = fExperiment.seekEvent(fExperiment.getNbEvents() - 1);
        fExperiment.getNext(context1);
        ITmfLocation<?> location = context1.getLocation().clone();
        ITmfContext context2 = fExperiment.seekEvent(location);
        ITmfEvent event = fExperiment.getNext(context2);
        assertNull("Event", event);
    }

    // ------------------------------------------------------------------------
    // processRequest
    // ------------------------------------------------------------------------

    public void testProcessRequestForNbEvents() throws Exception {

        final int blockSize = 100;
        final int nbEvents  = 1000;
        final Vector<TmfEvent> requestedEvents = new Vector<TmfEvent>();

        final TmfTimeRange range = new TmfTimeRange(TmfTimestamp.BIG_BANG, TmfTimestamp.BIG_CRUNCH);
        final TmfEventRequest<TmfEvent> request = new TmfEventRequest<TmfEvent>(TmfEvent.class, range, nbEvents, blockSize) {
            @Override
            public void handleData(final TmfEvent event) {
                super.handleData(event);
                requestedEvents.add(event);
            }
        };
        fExperiment.sendRequest(request);
        request.waitForCompletion();

        assertEquals("nbEvents", nbEvents, requestedEvents.size());
        assertTrue("isCompleted",  request.isCompleted());
        assertFalse("isCancelled", request.isCancelled());

        // Ensure that we have distinct events.
        // Don't go overboard: we are not validating the stub!
        for (int i = 0; i < nbEvents; i++) {
            assertEquals("Distinct events", i+1, requestedEvents.get(i).getTimestamp().getValue());
        }
    }

    public void testProcessRequestForNbEvents2() throws Exception {

        final int blockSize = 2 * NB_EVENTS;
        final int nbEvents = 1000;
        final Vector<TmfEvent> requestedEvents = new Vector<TmfEvent>();

        final TmfTimeRange range = new TmfTimeRange(TmfTimestamp.BIG_BANG, TmfTimestamp.BIG_CRUNCH);
        final TmfEventRequest<TmfEvent> request = new TmfEventRequest<TmfEvent>(TmfEvent.class, range, nbEvents, blockSize) {
            @Override
            public void handleData(final TmfEvent event) {
                super.handleData(event);
                requestedEvents.add(event);
            }
        };
        fExperiment.sendRequest(request);
        request.waitForCompletion();

        assertEquals("nbEvents", nbEvents, requestedEvents.size());
        assertTrue("isCompleted",  request.isCompleted());
        assertFalse("isCancelled", request.isCancelled());

        // Ensure that we have distinct events.
        // Don't go overboard: we are not validating the stub!
        for (int i = 0; i < nbEvents; i++) {
            assertEquals("Distinct events", i+1, requestedEvents.get(i).getTimestamp().getValue());
        }
    }

    public void testProcessRequestForAllEvents() throws Exception {

        final int nbEvents  = TmfDataRequest.ALL_DATA;
        final int blockSize =  1;
        final Vector<TmfEvent> requestedEvents = new Vector<TmfEvent>();
        final long nbExpectedEvents = NB_EVENTS;

        final TmfTimeRange range = new TmfTimeRange(TmfTimestamp.BIG_BANG, TmfTimestamp.BIG_CRUNCH);
        final TmfEventRequest<TmfEvent> request = new TmfEventRequest<TmfEvent>(TmfEvent.class, range, nbEvents, blockSize) {
            @Override
            public void handleData(final TmfEvent event) {
                super.handleData(event);
                requestedEvents.add(event);
            }
        };
        fExperiment.sendRequest(request);
        request.waitForCompletion();

        assertEquals("nbEvents", nbExpectedEvents, requestedEvents.size());
        assertTrue("isCompleted",  request.isCompleted());
        assertFalse("isCancelled", request.isCancelled());

        // Ensure that we have distinct events.
        // Don't go overboard: we are not validating the stub!
        for (int i = 0; i < nbExpectedEvents; i++) {
            assertEquals("Distinct events", i+1, requestedEvents.get(i).getTimestamp().getValue());
        }
    }

    // ------------------------------------------------------------------------
    // cancel
    // ------------------------------------------------------------------------

    public void testCancel() throws Exception {

        final int nbEvents  = NB_EVENTS;
        final int blockSize = BLOCK_SIZE;
        final Vector<TmfEvent> requestedEvents = new Vector<TmfEvent>();

        final TmfTimeRange range = new TmfTimeRange(TmfTimestamp.BIG_BANG, TmfTimestamp.BIG_CRUNCH);
        final TmfEventRequest<TmfEvent> request = new TmfEventRequest<TmfEvent>(TmfEvent.class, range, nbEvents, blockSize) {
            int nbRead = 0;
            @Override
            public void handleData(final TmfEvent event) {
                super.handleData(event);
                requestedEvents.add(event);
                if (++nbRead == blockSize) {
                    cancel();
                }
            }
            @Override
            public void handleCancel() {
                if (requestedEvents.size() < blockSize) {
                    System.out.println("aie");
                }
            }
        };
        fExperiment.sendRequest(request);
        request.waitForCompletion();

        assertEquals("nbEvents",  blockSize, requestedEvents.size());
        assertTrue("isCompleted", request.isCompleted());
        assertTrue("isCancelled", request.isCancelled());
    }

    // ------------------------------------------------------------------------
    // getTimestamp
    // ------------------------------------------------------------------------

    public void testGetTimestamp() throws Exception {
        assertEquals("getTimestamp", new TmfTimestamp(    1, (byte) -3), fExperiment.getTimestamp(    0));
        assertEquals("getTimestamp", new TmfTimestamp(    2, (byte) -3), fExperiment.getTimestamp(    1));
        assertEquals("getTimestamp", new TmfTimestamp(   11, (byte) -3), fExperiment.getTimestamp(   10));
        assertEquals("getTimestamp", new TmfTimestamp(  101, (byte) -3), fExperiment.getTimestamp(  100));
        assertEquals("getTimestamp", new TmfTimestamp( 1001, (byte) -3), fExperiment.getTimestamp( 1000));
        assertEquals("getTimestamp", new TmfTimestamp( 2001, (byte) -3), fExperiment.getTimestamp( 2000));
        assertEquals("getTimestamp", new TmfTimestamp( 2501, (byte) -3), fExperiment.getTimestamp( 2500));
        assertEquals("getTimestamp", new TmfTimestamp(10000, (byte) -3), fExperiment.getTimestamp( 9999));
        assertEquals("getTimestamp", new TmfTimestamp(20000, (byte) -3), fExperiment.getTimestamp(19999));
        assertNull("getTimestamp", fExperiment.getTimestamp(20000));
    }

}
