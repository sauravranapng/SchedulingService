package com.saurav.schedulingService.controller;

import com.saurav.schedulingService.leader.LeaderElectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class leaderController {
    @Autowired
    private LeaderElectionService leaderElectionService;

    @GetMapping("/is-leader")
    public String isLeader() {
        return leaderElectionService.isLeader() ? "I am the leader" : "I am a follower";
    }
}
