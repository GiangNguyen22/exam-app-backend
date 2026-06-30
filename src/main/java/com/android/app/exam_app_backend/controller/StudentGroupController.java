package com.android.app.exam_app_backend.controller;

import com.android.app.exam_app_backend.common.dto.ApiResponse;
import com.android.app.exam_app_backend.entity.StudentGroup;
import com.android.app.exam_app_backend.entity.StudentGroupMember;
import com.android.app.exam_app_backend.payload.StudentGroupResponse;
import com.android.app.exam_app_backend.payload.StudentGroupMemberResponse;
import com.android.app.exam_app_backend.payload.GroupCreateRequest;
import com.android.app.exam_app_backend.service.StudentGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class StudentGroupController {

    private final StudentGroupService groupService;

    public StudentGroupController(StudentGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentGroupResponse>>> getAllGroups() {
        List<StudentGroupResponse> groups = groupService.getAllGroups().stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<StudentGroupResponse>>builder()
                .success(true).code(HttpStatus.OK.value()).message("Groups retrieved").data(groups).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentGroupResponse>> getGroup(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<StudentGroupResponse>builder()
                .success(true).code(HttpStatus.OK.value()).message("Group retrieved")
                .data(toGroupResponse(groupService.getGroup(id))).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentGroupResponse>> createGroup(@RequestBody GroupCreateRequest request) {
        StudentGroup group = groupService.createGroup(request.getName(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.<StudentGroupResponse>builder()
                .success(true).code(HttpStatus.CREATED.value()).message("Group created")
                .data(toGroupResponse(group)).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentGroupResponse>> updateGroup(@PathVariable Long id,
                                                                          @RequestBody GroupCreateRequest request) {
        StudentGroup group = groupService.updateGroup(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(ApiResponse.<StudentGroupResponse>builder()
                .success(true).code(HttpStatus.OK.value()).message("Group updated")
                .data(toGroupResponse(group)).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true).code(HttpStatus.OK.value()).message("Group deleted").build());
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<StudentGroupMemberResponse>>> getMembers(@PathVariable Long id) {
        List<StudentGroupMemberResponse> members = groupService.getMembers(id).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<StudentGroupMemberResponse>>builder()
                .success(true).code(HttpStatus.OK.value()).message("Members retrieved").data(members).build());
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<StudentGroupMemberResponse>>> addMember(@PathVariable Long id,
                                                                                   @RequestBody MemberRequest request) {
        List<StudentGroupMemberResponse> members = groupService.addMembers(id, request.getUserIds()).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<StudentGroupMemberResponse>>builder()
                .success(true).code(HttpStatus.CREATED.value()).message("Members added")
                .data(members).build());
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<String>> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        groupService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true).code(HttpStatus.OK.value()).message("Member removed").build());
    }

    private StudentGroupResponse toGroupResponse(StudentGroup group) {
        return StudentGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .memberCount(group.getMembers().size())
                .build();
    }

    private StudentGroupMemberResponse toMemberResponse(StudentGroupMember member) {
        return StudentGroupMemberResponse.builder()
                .id(member.getId())
                .groupId(member.getGroup().getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .fullName(member.getUser().getFullName())
                .build();
    }

    static class MemberRequest {
        private List<Long> userIds;
        public List<Long> getUserIds() { return userIds; }
        public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    }
}
