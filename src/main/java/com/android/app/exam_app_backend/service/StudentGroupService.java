package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.entity.StudentGroup;
import com.android.app.exam_app_backend.entity.StudentGroupMember;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.repository.StudentGroupMemberRepository;
import com.android.app.exam_app_backend.repository.StudentGroupRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentGroupService {

    private final StudentGroupRepository groupRepository;
    private final StudentGroupMemberRepository memberRepository;
    private final UserRepository userRepository;

    public StudentGroupService(StudentGroupRepository groupRepository,
                               StudentGroupMemberRepository memberRepository,
                               UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    public List<StudentGroup> getAllGroups() {
        return groupRepository.findAll();
    }

    public StudentGroup getGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
    }

    public StudentGroup createGroup(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required.");
        }
        StudentGroup group = new StudentGroup();
        group.setName(name.trim());
        group.setDescription(description);
        return groupRepository.save(group);
    }

    public StudentGroup updateGroup(Long id, String name, String description) {
        StudentGroup group = getGroup(id);
        if (name != null && !name.trim().isEmpty()) {
            group.setName(name.trim());
        }
        if (description != null) {
            group.setDescription(description);
        }
        return groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        StudentGroup group = getGroup(id);
        groupRepository.delete(group);
    }

    public List<StudentGroupMember> getMembers(Long groupId) {
        getGroup(groupId); // ensure exists
        return memberRepository.findByGroupId(groupId);
    }

    @Transactional
    public StudentGroupMember addMember(Long groupId, Long userId) {
        StudentGroup group = getGroup(groupId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        if (memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already in group.");
        }
        StudentGroupMember member = new StudentGroupMember();
        member.setGroup(group);
        member.setUser(user);
        return memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        StudentGroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));
        memberRepository.delete(member);
    }

    public List<Long> getUserGroupIds(Long userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(m -> m.getGroup().getId())
                .collect(Collectors.toList());
    }
}
