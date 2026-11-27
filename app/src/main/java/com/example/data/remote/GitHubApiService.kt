package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(): GitHubUserDto

    @GET("user/repos")
    suspend fun getAuthenticatedUserRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member",
    ): List<GitHubRepoDto>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubRepoDto

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 50,
    ): List<GitHubBranchDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContentsList(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String = "",
        @Query("ref") ref: String? = null,
    ): List<GitHubContentItemDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String? = null,
    ): GitHubContentItemDto

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: PutFileRequestDto,
    ): PutFileResponseDto

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun getWorkflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 50,
    ): GitHubWorkflowsResponseDto

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: Long,
        @Body body: WorkflowDispatchRequestDto,
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20,
    ): GitHubRunsResponseDto

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun getRunJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): GitHubJobsResponseDto

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/artifacts")
    suspend fun getRepositoryArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
    ): GitHubArtifactsResponseDto

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getRunArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long,
    ): GitHubArtifactsResponseDto

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 15,
    ): List<GitHubReleaseDto>
}
