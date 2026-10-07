package com.bharatanpr.tracking

import org.junit.Assert.*
import org.junit.Test

class TrackingTest {
    @Test fun weightedVotingSelectsRepeatedStrongCandidate(){val v=TemporalVoting();v.add(o("TN01AB1234",.88f));v.add(o("TN01A81234",.74f));val r=v.add(o("TN01AB1234",.95f));val final=v.add(o("TN01AB1234",.96f));assertEquals("TN01AB1234",r.value);assertTrue(final.finalized);assertTrue(final.confidence>.82f)}
    @Test fun duplicateCooldownUsesElapsedTime(){val d=DuplicateSuppressor(20_000);assertTrue(d.shouldAccept("TN01AB1234",1_000));assertFalse(d.shouldAccept("TN01AB1234",19_000));assertTrue(d.shouldAccept("TN01AB1234",21_001))}
    private fun o(v:String,c:Float)=VoteObservation(v,c,.94f,.9f,.97f)
}
