package io.github.mirkoscotti.nio.s3.enums;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * List of S3 actions based on
 * <a href="https://docs.aws.amazon.com/service-authorization/latest/reference/list_amazons3.html">
 * AWS Service Authorization Reference for Amazon S3</a>
 *
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public enum BucketAction
{

	/**
	 * Maps the <code>s3:AbortMultipartUpload</code> bucket policy action.
	 */
	S3_ABORT_MULTIPART_UPLOAD("s3:AbortMultipartUpload", false),
	/**
	 * Maps the <code>s3:AssociateAccessGrantsIdentityCenter</code> bucket policy action.
	 */
	S3_ASSOCIATE_ACCESS_GRANTS_IDENTITY_CENTER("s3:AssociateAccessGrantsIdentityCenter", false),
	/**
	 * Maps the <code>s3:BypassGovernanceRetention</code> bucket policy action.
	 */
	S3_BYPASS_GOVERNANCE_RETENTION("s3:BypassGovernanceRetention", false),
	/**
	 * Maps the <code>s3:CreateAccessGrant</code> bucket policy action.
	 */
	S3_CREATE_ACCESS_GRANT("s3:CreateAccessGrant", false),
	/**
	 * Maps the <code>s3:CreateAccessGrantsInstance</code> bucket policy action.
	 */
	S3_CREATE_ACCESS_GRANTS_INSTANCE("s3:CreateAccessGrantsInstance", false),
	/**
	 * Maps the <code>s3:CreateAccessGrantsLocation</code> bucket policy action.
	 */
	S3_CREATE_ACCESS_GRANTS_LOCATION("s3:CreateAccessGrantsLocation", false),
	/**
	 * Maps the <code>s3:CreateAccessPoint</code> bucket policy action.
	 */
	S3_CREATE_ACCESS_POINT("s3:CreateAccessPoint", false),
	/**
	 * Maps the <code>s3:CreateAccessPointForObjectLambda</code> bucket policy action.
	 */
	S3_CREATE_ACCESS_POINT_FOR_OBJECT_LAMBDA("s3:CreateAccessPointForObjectLambda", false),
	/**
	 * Maps the <code>s3:CreateBucket</code> bucket policy action.
	 */
	S3_CREATE_BUCKET("s3:CreateBucket", false),
	/**
	 * Maps the <code>s3:CreateBucketMetadataTableConfiguration</code> bucket policy action.
	 */
	S3_CREATE_BUCKET_METADATA_TABLE_CONFIGURATION(
		"s3:CreateBucketMetadataTableConfiguration",
		false),
	/**
	 * Maps the <code>s3:CreateJob</code> bucket policy action.
	 */
	S3_CREATE_JOB("s3:CreateJob", false),
	/**
	 * Maps the <code>s3:CreateMultiRegionAccessPoint</code> bucket policy action.
	 */
	S3_CREATE_MULTI_REGION_ACCESS_POINT("s3:CreateMultiRegionAccessPoint", false),
	/**
	 * Maps the <code>s3:CreateStorageLensGroup</code> bucket policy action.
	 */
	S3_CREATE_STORAGE_LENS_GROUP("s3:CreateStorageLensGroup", false),
	/**
	 * Maps the <code>s3:DeleteAccessGrant</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_GRANT("s3:DeleteAccessGrant", false),
	/**
	 * Maps the <code>s3:DeleteAccessGrantsInstance</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_GRANTS_INSTANCE("s3:DeleteAccessGrantsInstance", false),
	/**
	 * Maps the <code>s3:DeleteAccessGrantsInstanceResourcePolicy</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_GRANTS_INSTANCE_RESOURCE_POLICY(
		"s3:DeleteAccessGrantsInstanceResourcePolicy",
		false),
	/**
	 * Maps the <code>s3:DeleteAccessGrantsLocation</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_GRANTS_LOCATION("s3:DeleteAccessGrantsLocation", false),
	/**
	 * Maps the <code>s3:DeleteAccessPoint</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_POINT("s3:DeleteAccessPoint", false),
	/**
	 * Maps the <code>s3:DeleteAccessPointForObjectLambda</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_POINT_FOR_OBJECT_LAMBDA("s3:DeleteAccessPointForObjectLambda", false),
	/**
	 * Maps the <code>s3:DeleteAccessPointPolicy</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_POINT_POLICY("s3:DeleteAccessPointPolicy", false),
	/**
	 * Maps the <code>s3:DeleteAccessPointPolicyForObjectLambda</code> bucket policy action.
	 */
	S3_DELETE_ACCESS_POINT_POLICY_FOR_OBJECT_LAMBDA(
		"s3:DeleteAccessPointPolicyForObjectLambda",
		false),
	/**
	 * Maps the <code>s3:DeleteBucket</code> bucket policy action.
	 */
	S3_DELETE_BUCKET("s3:DeleteBucket", false),
	/**
	 * Maps the <code>s3:DeleteBucketMetadataTableConfiguration</code> bucket policy action.
	 */
	S3_DELETE_BUCKET_METADATA_TABLE_CONFIGURATION(
		"s3:DeleteBucketMetadataTableConfiguration",
		false),
	/**
	 * Maps the <code>s3:DeleteBucketPolicy</code> bucket policy action.
	 */
	S3_DELETE_BUCKET_POLICY("s3:DeleteBucketPolicy", false),
	/**
	 * Maps the <code>s3:DeleteBucketWebsite</code> bucket policy action.
	 */
	S3_DELETE_BUCKET_WEBSITE("s3:DeleteBucketWebsite", false),
	/**
	 * Maps the <code>s3:DeleteJobTagging</code> bucket policy action.
	 */
	S3_DELETE_JOB_TAGGING("s3:DeleteJobTagging", false),
	/**
	 * Maps the <code>s3:DeleteMultiRegionAccessPoint</code> bucket policy action.
	 */
	S3_DELETE_MULTI_REGION_ACCESS_POINT("s3:DeleteMultiRegionAccessPoint", false),
	/**
	 * Maps the <code>s3:DeleteObject</code> bucket policy action.
	 */
	S3_DELETE_OBJECT("s3:DeleteObject", false),
	/**
	 * Maps the <code>s3:DeleteObjectTagging</code> bucket policy action.
	 */
	S3_DELETE_OBJECT_TAGGING("s3:DeleteObjectTagging", false),
	/**
	 * Maps the <code>s3:DeleteObjectVersion</code> bucket policy action.
	 */
	S3_DELETE_OBJECT_VERSION("s3:DeleteObjectVersion", false),
	/**
	 * Maps the <code>s3:DeleteObjectVersionTagging</code> bucket policy action.
	 */
	S3_DELETE_OBJECT_VERSION_TAGGING("s3:DeleteObjectVersionTagging", false),
	/**
	 * Maps the <code>s3:DeleteStorageLensConfiguration</code> bucket policy action.
	 */
	S3_DELETE_STORAGE_LENS_CONFIGURATION("s3:DeleteStorageLensConfiguration", false),
	/**
	 * Maps the <code>s3:DeleteStorageLensConfigurationTagging</code> bucket policy action.
	 */
	S3_DELETE_STORAGE_LENS_CONFIGURATION_TAGGING("s3:DeleteStorageLensConfigurationTagging", false),
	/**
	 * Maps the <code>s3:DeleteStorageLensGroup</code> bucket policy action.
	 */
	S3_DELETE_STORAGE_LENS_GROUP("s3:DeleteStorageLensGroup", false),
	/**
	 * Maps the <code>s3:DescribeJob</code> bucket policy action.
	 */
	S3_DESCRIBE_JOB("s3:DescribeJob", true),
	/**
	 * Maps the <code>s3:DescribeMultiRegionAccessPointOperation</code> bucket policy action.
	 */
	S3_DESCRIBE_MULTI_REGION_ACCESS_POINT_OPERATION(
		"s3:DescribeMultiRegionAccessPointOperation",
		true),
	/**
	 * Maps the <code>s3:DissociateAccessGrantsIdentityCenter</code> bucket policy action.
	 */
	S3_DISSOCIATE_ACCESS_GRANTS_IDENTITY_CENTER("s3:DissociateAccessGrantsIdentityCenter", false),
	/**
	 * Maps the <code>s3:GetAccelerateConfiguration</code> bucket policy action.
	 */
	S3_GET_ACCELERATE_CONFIGURATION("s3:GetAccelerateConfiguration", true),
	/**
	 * Maps the <code>s3:GetAccessGrant</code> bucket policy action.
	 */
	S3_GET_ACCESS_GRANT("s3:GetAccessGrant", true),
	/**
	 * Maps the <code>s3:GetAccessGrantsInstance</code> bucket policy action.
	 */
	S3_GET_ACCESS_GRANTS_INSTANCE("s3:GetAccessGrantsInstance", true),
	/**
	 * Maps the <code>s3:GetAccessGrantsInstanceForPrefix</code> bucket policy action.
	 */
	S3_GET_ACCESS_GRANTS_INSTANCE_FOR_PREFIX("s3:GetAccessGrantsInstanceForPrefix", true),
	/**
	 * Maps the <code>s3:GetAccessGrantsInstanceResourcePolicy</code> bucket policy action.
	 */
	S3_GET_ACCESS_GRANTS_INSTANCE_RESOURCE_POLICY("s3:GetAccessGrantsInstanceResourcePolicy", true),
	/**
	 * Maps the <code>s3:GetAccessGrantsLocation</code> bucket policy action.
	 */
	S3_GET_ACCESS_GRANTS_LOCATION("s3:GetAccessGrantsLocation", true),
	/**
	 * Maps the <code>s3:GetAccessPoint</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT("s3:GetAccessPoint", true),
	/**
	 * Maps the <code>s3:GetAccessPointConfigurationForObjectLambda</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT_CONFIGURATION_FOR_OBJECT_LAMBDA(
		"s3:GetAccessPointConfigurationForObjectLambda",
		true),
	/**
	 * Maps the <code>s3:GetAccessPointForObjectLambda</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT_FOR_OBJECT_LAMBDA("s3:GetAccessPointForObjectLambda", true),
	/**
	 * Maps the <code>s3:GetAccessPointPolicy</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT_POLICY("s3:GetAccessPointPolicy", true),
	/**
	 * Maps the <code>s3:GetAccessPointPolicyForObjectLambda</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT_POLICY_FOR_OBJECT_LAMBDA("s3:GetAccessPointPolicyForObjectLambda", true),
	/**
	 * Maps the <code>s3:GetAccessPointPolicyStatus</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT_POLICY_STATUS("s3:GetAccessPointPolicyStatus", true),
	/**
	 * Maps the <code>s3:GetAccessPointPolicyStatusForObjectLambda</code> bucket policy action.
	 */
	S3_GET_ACCESS_POINT_POLICY_STATUS_FOR_OBJECT_LAMBDA(
		"s3:GetAccessPointPolicyStatusForObjectLambda",
		true),
	/**
	 * Maps the <code>s3:GetAccountPublicAccessBlock</code> bucket policy action.
	 */
	S3_GET_ACCOUNT_PUBLIC_ACCESS_BLOCK("s3:GetAccountPublicAccessBlock", true),
	/**
	 * Maps the <code>s3:GetAnalyticsConfiguration</code> bucket policy action.
	 */
	S3_GET_ANALYTICS_CONFIGURATION("s3:GetAnalyticsConfiguration", true),
	/**
	 * Maps the <code>s3:GetBucketAcl</code> bucket policy action.
	 */
	S3_GET_BUCKET_ACL("s3:GetBucketAcl", true),
	/**
	 * Maps the <code>s3:GetBucketCORS</code> bucket policy action.
	 */
	S3_GET_BUCKET_CORS("s3:GetBucketCORS", true),
	/**
	 * Maps the <code>s3:GetBucketLocation</code> bucket policy action.
	 */
	S3_GET_BUCKET_LOCATION("s3:GetBucketLocation", true),
	/**
	 * Maps the <code>s3:GetBucketLogging</code> bucket policy action.
	 */
	S3_GET_BUCKET_LOGGING("s3:GetBucketLogging", true),
	/**
	 * Maps the <code>s3:GetBucketMetadataTableConfiguration</code> bucket policy action.
	 */
	S3_GET_BUCKET_METADATA_TABLE_CONFIGURATION("s3:GetBucketMetadataTableConfiguration", true),
	/**
	 * Maps the <code>s3:GetBucketNotification</code> bucket policy action.
	 */
	S3_GET_BUCKET_NOTIFICATION("s3:GetBucketNotification", true),
	/**
	 * Maps the <code>s3:GetBucketObjectLockConfiguration</code> bucket policy action.
	 */
	S3_GET_BUCKET_OBJECT_LOCK_CONFIGURATION("s3:GetBucketObjectLockConfiguration", true),
	/**
	 * Maps the <code>s3:GetBucketOwnershipControls</code> bucket policy action.
	 */
	S3_GET_BUCKET_OWNERSHIP_CONTROLS("s3:GetBucketOwnershipControls", true),
	/**
	 * Maps the <code>s3:GetBucketPolicy</code> bucket policy action.
	 */
	S3_GET_BUCKET_POLICY("s3:GetBucketPolicy", true),
	/**
	 * Maps the <code>s3:GetBucketPolicyStatus</code> bucket policy action.
	 */
	S3_GET_BUCKET_POLICY_STATUS("s3:GetBucketPolicyStatus", true),
	/**
	 * Maps the <code>s3:GetBucketPublicAccessBlock</code> bucket policy action.
	 */
	S3_GET_BUCKET_PUBLIC_ACCESS_BLOCK("s3:GetBucketPublicAccessBlock", true),
	/**
	 * Maps the <code>s3:GetBucketRequestPayment</code> bucket policy action.
	 */
	S3_GET_BUCKET_REQUEST_PAYMENT("s3:GetBucketRequestPayment", true),
	/**
	 * Maps the <code>s3:GetBucketTagging</code> bucket policy action.
	 */
	S3_GET_BUCKET_TAGGING("s3:GetBucketTagging", true),
	/**
	 * Maps the <code>s3:GetBucketVersioning</code> bucket policy action.
	 */
	S3_GET_BUCKET_VERSIONING("s3:GetBucketVersioning", true),
	/**
	 * Maps the <code>s3:GetBucketWebsite</code> bucket policy action.
	 */
	S3_GET_BUCKET_WEBSITE("s3:GetBucketWebsite", true),
	/**
	 * Maps the <code>s3:GetDataAccess</code> bucket policy action.
	 */
	S3_GET_DATA_ACCESS("s3:GetDataAccess", true),
	/**
	 * Maps the <code>s3:GetEncryptionConfiguration</code> bucket policy action.
	 */
	S3_GET_ENCRYPTION_CONFIGURATION("s3:GetEncryptionConfiguration", true),
	/**
	 * Maps the <code>s3:GetIntelligentTieringConfiguration</code> bucket policy action.
	 */
	S3_GET_INTELLIGENT_TIERING_CONFIGURATION("s3:GetIntelligentTieringConfiguration", true),
	/**
	 * Maps the <code>s3:GetInventoryConfiguration</code> bucket policy action.
	 */
	S3_GET_INVENTORY_CONFIGURATION("s3:GetInventoryConfiguration", true),
	/**
	 * Maps the <code>s3:GetJobTagging</code> bucket policy action.
	 */
	S3_GET_JOB_TAGGING("s3:GetJobTagging", true),
	/**
	 * Maps the <code>s3:GetLifecycleConfiguration</code> bucket policy action.
	 */
	S3_GET_LIFECYCLE_CONFIGURATION("s3:GetLifecycleConfiguration", true),
	/**
	 * Maps the <code>s3:GetMetricsConfiguration</code> bucket policy action.
	 */
	S3_GET_METRICS_CONFIGURATION("s3:GetMetricsConfiguration", true),
	/**
	 * Maps the <code>s3:GetMultiRegionAccessPoint</code> bucket policy action.
	 */
	S3_GET_MULTI_REGION_ACCESS_POINT("s3:GetMultiRegionAccessPoint", true),
	/**
	 * Maps the <code>s3:GetMultiRegionAccessPointPolicy</code> bucket policy action.
	 */
	S3_GET_MULTI_REGION_ACCESS_POINT_POLICY("s3:GetMultiRegionAccessPointPolicy", true),
	/**
	 * Maps the <code>s3:GetMultiRegionAccessPointPolicyStatus</code> bucket policy action.
	 */
	S3_GET_MULTI_REGION_ACCESS_POINT_POLICY_STATUS(
		"s3:GetMultiRegionAccessPointPolicyStatus",
		true),
	/**
	 * Maps the <code>s3:GetMultiRegionAccessPointRoutes</code> bucket policy action.
	 */
	S3_GET_MULTI_REGION_ACCESS_POINT_ROUTES("s3:GetMultiRegionAccessPointRoutes", true),
	/**
	 * Maps the <code>s3:GetObject</code> bucket policy action.
	 */
	S3_GET_OBJECT("s3:GetObject", true),
	/**
	 * Maps the <code>s3:GetObjectAcl</code> bucket policy action.
	 */
	S3_GET_OBJECT_ACL("s3:GetObjectAcl", true),
	/**
	 * Maps the <code>s3:GetObjectAttributes</code> bucket policy action.
	 */
	S3_GET_OBJECT_ATTRIBUTES("s3:GetObjectAttributes", true),
	/**
	 * Maps the <code>s3:GetObjectLegalHold</code> bucket policy action.
	 */
	S3_GET_OBJECT_LEGAL_HOLD("s3:GetObjectLegalHold", true),
	/**
	 * Maps the <code>s3:GetObjectRetention</code> bucket policy action.
	 */
	S3_GET_OBJECT_RETENTION("s3:GetObjectRetention", true),
	/**
	 * Maps the <code>s3:GetObjectTagging</code> bucket policy action.
	 */
	S3_GET_OBJECT_TAGGING("s3:GetObjectTagging", true),
	/**
	 * Maps the <code>s3:GetObjectTorrent</code> bucket policy action.
	 */
	S3_GET_OBJECT_TORRENT("s3:GetObjectTorrent", true),
	/**
	 * Maps the <code>s3:GetObjectVersion</code> bucket policy action.
	 */
	S3_GET_OBJECT_VERSION("s3:GetObjectVersion", true),
	/**
	 * Maps the <code>s3:GetObjectVersionAcl</code> bucket policy action.
	 */
	S3_GET_OBJECT_VERSION_ACL("s3:GetObjectVersionAcl", true),
	/**
	 * Maps the <code>s3:GetObjectVersionAttributes</code> bucket policy action.
	 */
	S3_GET_OBJECT_VERSION_ATTRIBUTES("s3:GetObjectVersionAttributes", true),
	/**
	 * Maps the <code>s3:GetObjectVersionForReplication</code> bucket policy action.
	 */
	S3_GET_OBJECT_VERSION_FOR_REPLICATION("s3:GetObjectVersionForReplication", true),
	/**
	 * Maps the <code>s3:GetObjectVersionTagging</code> bucket policy action.
	 */
	S3_GET_OBJECT_VERSION_TAGGING("s3:GetObjectVersionTagging", true),
	/**
	 * Maps the <code>s3:GetObjectVersionTorrent</code> bucket policy action.
	 */
	S3_GET_OBJECT_VERSION_TORRENT("s3:GetObjectVersionTorrent", true),
	/**
	 * Maps the <code>s3:GetReplicationConfiguration</code> bucket policy action.
	 */
	S3_GET_REPLICATION_CONFIGURATION("s3:GetReplicationConfiguration", true),
	/**
	 * Maps the <code>s3:GetStorageClassAnalysis</code> bucket policy action.
	 */
	S3_GET_STORAGE_CLASS_ANALYSIS("s3:GetStorageClassAnalysis", true),
	/**
	 * Maps the <code>s3:GetStorageLensConfiguration</code> bucket policy action.
	 */
	S3_GET_STORAGE_LENS_CONFIGURATION("s3:GetStorageLensConfiguration", true),
	/**
	 * Maps the <code>s3:GetStorageLensConfigurationTagging</code> bucket policy action.
	 */
	S3_GET_STORAGE_LENS_CONFIGURATION_TAGGING("s3:GetStorageLensConfigurationTagging", true),
	/**
	 * Maps the <code>s3:GetStorageLensDashboard</code> bucket policy action.
	 */
	S3_GET_STORAGE_LENS_DASHBOARD("s3:GetStorageLensDashboard", true),
	/**
	 * Maps the <code>s3:GetStorageLensGroup</code> bucket policy action.
	 */
	S3_GET_STORAGE_LENS_GROUP("s3:GetStorageLensGroup", true),
	/**
	 * Maps the <code>s3:HeadBucket</code> bucket policy action.
	 */
	S3_HEAD_BUCKET("s3:HeadBucket", true),
	/**
	 * Maps the <code>s3:HeadObject</code> bucket policy action.
	 */
	S3_HEAD_OBJECT("s3:HeadObject", true),
	/**
	 * Maps the <code>s3:InitiateReplication</code> bucket policy action.
	 */
	S3_INITIATE_REPLICATION("s3:InitiateReplication", false),
	/**
	 * Maps the <code>s3:ListAccessGrants</code> bucket policy action.
	 */
	S3_LIST_ACCESS_GRANTS("s3:ListAccessGrants", true),
	/**
	 * Maps the <code>s3:ListAccessGrantsInstances</code> bucket policy action.
	 */
	S3_LIST_ACCESS_GRANTS_INSTANCES("s3:ListAccessGrantsInstances", true),
	/**
	 * Maps the <code>s3:ListAccessGrantsLocations</code> bucket policy action.
	 */
	S3_LIST_ACCESS_GRANTS_LOCATIONS("s3:ListAccessGrantsLocations", true),
	/**
	 * Maps the <code>s3:ListAccessPoints</code> bucket policy action.
	 */
	S3_LIST_ACCESS_POINTS("s3:ListAccessPoints", true),
	/**
	 * Maps the <code>s3:ListAccessPointsForObjectLambda</code> bucket policy action.
	 */
	S3_LIST_ACCESS_POINTS_FOR_OBJECT_LAMBDA("s3:ListAccessPointsForObjectLambda", true),
	/**
	 * Maps the <code>s3:ListAllMyBuckets</code> bucket policy action.
	 */
	S3_LIST_ALL_MY_BUCKETS("s3:ListAllMyBuckets", true),
	/**
	 * Maps the <code>s3:ListBucket</code> bucket policy action.
	 */
	S3_LIST_BUCKET("s3:ListBucket", true),
	/**
	 * Maps the <code>s3:ListBucketMultipartUploads</code> bucket policy action.
	 */
	S3_LIST_BUCKET_MULTIPART_UPLOADS("s3:ListBucketMultipartUploads", true),
	/**
	 * Maps the <code>s3:ListBucketVersions</code> bucket policy action.
	 */
	S3_LIST_BUCKET_VERSIONS("s3:ListBucketVersions", true),
	/**
	 * Maps the <code>s3:ListCallerAccessGrants</code> bucket policy action.
	 */
	S3_LIST_CALLER_ACCESS_GRANTS("s3:ListCallerAccessGrants", true),
	/**
	 * Maps the <code>s3:ListJobs</code> bucket policy action.
	 */
	S3_LIST_JOBS("s3:ListJobs", true),
	/**
	 * Maps the <code>s3:ListMultiRegionAccessPoints</code> bucket policy action.
	 */
	S3_LIST_MULTI_REGION_ACCESS_POINTS("s3:ListMultiRegionAccessPoints", true),
	/**
	 * Maps the <code>s3:ListMultipartUploadParts</code> bucket policy action.
	 */
	S3_LIST_MULTIPART_UPLOAD_PARTS("s3:ListMultipartUploadParts", true),
	/**
	 * Maps the <code>s3:ListStorageClassAnalysis</code> bucket policy action.
	 */
	S3_LIST_STORAGE_CLASS_ANALYSIS("s3:ListStorageClassAnalysis", true),
	/**
	 * Maps the <code>s3:ListStorageLensConfigurations</code> bucket policy action.
	 */
	S3_LIST_STORAGE_LENS_CONFIGURATIONS("s3:ListStorageLensConfigurations", true),
	/**
	 * Maps the <code>s3:ListStorageLensGroups</code> bucket policy action.
	 */
	S3_LIST_STORAGE_LENS_GROUPS("s3:ListStorageLensGroups", true),
	/**
	 * Maps the <code>s3:ListTagsForResource</code> bucket policy action.
	 */
	S3_LIST_TAGS_FOR_RESOURCE("s3:ListTagsForResource", true),
	/**
	 * Maps the <code>s3:ObjectOwnerOverrideToBucketOwner</code> bucket policy action.
	 */
	S3_OBJECT_OWNER_OVERRIDE_TO_BUCKET_OWNER("s3:ObjectOwnerOverrideToBucketOwner", false),
	/**
	 * Maps the <code>s3:PauseReplication</code> bucket policy action.
	 */
	S3_PAUSE_REPLICATION("s3:PauseReplication", false),
	/**
	 * Maps the <code>s3:PutAccelerateConfiguration</code> bucket policy action.
	 */
	S3_PUT_ACCELERATE_CONFIGURATION("s3:PutAccelerateConfiguration", false),
	/**
	 * Maps the <code>s3:PutAccessGrantsInstanceResourcePolicy</code> bucket policy action.
	 */
	S3_PUT_ACCESS_GRANTS_INSTANCE_RESOURCE_POLICY(
		"s3:PutAccessGrantsInstanceResourcePolicy",
		false),
	/**
	 * Maps the <code>s3:PutAccessPointConfigurationForObjectLambda</code> bucket policy action.
	 */
	S3_PUT_ACCESS_POINT_CONFIGURATION_FOR_OBJECT_LAMBDA(
		"s3:PutAccessPointConfigurationForObjectLambda",
		false),
	/**
	 * Maps the <code>s3:PutAccessPointPolicy</code> bucket policy action.
	 */
	S3_PUT_ACCESS_POINT_POLICY("s3:PutAccessPointPolicy", false),
	/**
	 * Maps the <code>s3:PutAccessPointPolicyForObjectLambda</code> bucket policy action.
	 */
	S3_PUT_ACCESS_POINT_POLICY_FOR_OBJECT_LAMBDA("s3:PutAccessPointPolicyForObjectLambda", false),
	/**
	 * Maps the <code>s3:PutAccessPointPublicAccessBlock</code> bucket policy action.
	 */
	S3_PUT_ACCESS_POINT_PUBLIC_ACCESS_BLOCK("s3:PutAccessPointPublicAccessBlock", false),
	/**
	 * Maps the <code>s3:PutAccountPublicAccessBlock</code> bucket policy action.
	 */
	S3_PUT_ACCOUNT_PUBLIC_ACCESS_BLOCK("s3:PutAccountPublicAccessBlock", false),
	/**
	 * Maps the <code>s3:PutAnalyticsConfiguration</code> bucket policy action.
	 */
	S3_PUT_ANALYTICS_CONFIGURATION("s3:PutAnalyticsConfiguration", false),
	/**
	 * Maps the <code>s3:PutBucketAcl</code> bucket policy action.
	 */
	S3_PUT_BUCKET_ACL("s3:PutBucketAcl", false),
	/**
	 * Maps the <code>s3:PutBucketCORS</code> bucket policy action.
	 */
	S3_PUT_BUCKET_CORS("s3:PutBucketCORS", false),
	/**
	 * Maps the <code>s3:PutBucketLogging</code> bucket policy action.
	 */
	S3_PUT_BUCKET_LOGGING("s3:PutBucketLogging", false),
	/**
	 * Maps the <code>s3:PutBucketNotification</code> bucket policy action.
	 */
	S3_PUT_BUCKET_NOTIFICATION("s3:PutBucketNotification", false),
	/**
	 * Maps the <code>s3:PutBucketObjectLockConfiguration</code> bucket policy action.
	 */
	S3_PUT_BUCKET_OBJECT_LOCK_CONFIGURATION("s3:PutBucketObjectLockConfiguration", false),
	/**
	 * Maps the <code>s3:PutBucketOwnershipControls</code> bucket policy action.
	 */
	S3_PUT_BUCKET_OWNERSHIP_CONTROLS("s3:PutBucketOwnershipControls", false),
	/**
	 * Maps the <code>s3:PutBucketPolicy</code> bucket policy action.
	 */
	S3_PUT_BUCKET_POLICY("s3:PutBucketPolicy", false),
	/**
	 * Maps the <code>s3:PutBucketPublicAccessBlock</code> bucket policy action.
	 */
	S3_PUT_BUCKET_PUBLIC_ACCESS_BLOCK("s3:PutBucketPublicAccessBlock", false),
	/**
	 * Maps the <code>s3:PutBucketRequestPayment</code> bucket policy action.
	 */
	S3_PUT_BUCKET_REQUEST_PAYMENT("s3:PutBucketRequestPayment", false),
	/**
	 * Maps the <code>s3:PutBucketTagging</code> bucket policy action.
	 */
	S3_PUT_BUCKET_TAGGING("s3:PutBucketTagging", false),
	/**
	 * Maps the <code>s3:PutBucketVersioning</code> bucket policy action.
	 */
	S3_PUT_BUCKET_VERSIONING("s3:PutBucketVersioning", false),
	/**
	 * Maps the <code>s3:PutBucketWebsite</code> bucket policy action.
	 */
	S3_PUT_BUCKET_WEBSITE("s3:PutBucketWebsite", false),
	/**
	 * Maps the <code>s3:PutEncryptionConfiguration</code> bucket policy action.
	 */
	S3_PUT_ENCRYPTION_CONFIGURATION("s3:PutEncryptionConfiguration", false),
	/**
	 * Maps the <code>s3:PutIntelligentTieringConfiguration</code> bucket policy action.
	 */
	S3_PUT_INTELLIGENT_TIERING_CONFIGURATION("s3:PutIntelligentTieringConfiguration", false),
	/**
	 * Maps the <code>s3:PutInventoryConfiguration</code> bucket policy action.
	 */
	S3_PUT_INVENTORY_CONFIGURATION("s3:PutInventoryConfiguration", false),
	/**
	 * Maps the <code>s3:PutJobTagging</code> bucket policy action.
	 */
	S3_PUT_JOB_TAGGING("s3:PutJobTagging", false),
	/**
	 * Maps the <code>s3:PutLifecycleConfiguration</code> bucket policy action.
	 */
	S3_PUT_LIFECYCLE_CONFIGURATION("s3:PutLifecycleConfiguration", false),
	/**
	 * Maps the <code>s3:PutMetricsConfiguration</code> bucket policy action.
	 */
	S3_PUT_METRICS_CONFIGURATION("s3:PutMetricsConfiguration", false),
	/**
	 * Maps the <code>s3:PutMultiRegionAccessPointPolicy</code> bucket policy action.
	 */
	S3_PUT_MULTI_REGION_ACCESS_POINT_POLICY("s3:PutMultiRegionAccessPointPolicy", false),
	/**
	 * Maps the <code>s3:PutObject</code> bucket policy action.
	 */
	S3_PUT_OBJECT("s3:PutObject", false),
	/**
	 * Maps the <code>s3:PutObjectAcl</code> bucket policy action.
	 */
	S3_PUT_OBJECT_ACL("s3:PutObjectAcl", false),
	/**
	 * Maps the <code>s3:PutObjectLegalHold</code> bucket policy action.
	 */
	S3_PUT_OBJECT_LEGAL_HOLD("s3:PutObjectLegalHold", false),
	/**
	 * Maps the <code>s3:PutObjectRetention</code> bucket policy action.
	 */
	S3_PUT_OBJECT_RETENTION("s3:PutObjectRetention", false),
	/**
	 * Maps the <code>s3:PutObjectTagging</code> bucket policy action.
	 */
	S3_PUT_OBJECT_TAGGING("s3:PutObjectTagging", false),
	/**
	 * Maps the <code>s3:PutObjectVersionAcl</code> bucket policy action.
	 */
	S3_PUT_OBJECT_VERSION_ACL("s3:PutObjectVersionAcl", false),
	/**
	 * Maps the <code>s3:PutObjectVersionTagging</code> bucket policy action.
	 */
	S3_PUT_OBJECT_VERSION_TAGGING("s3:PutObjectVersionTagging", false),
	/**
	 * Maps the <code>s3:PutReplicationConfiguration</code> bucket policy action.
	 */
	S3_PUT_REPLICATION_CONFIGURATION("s3:PutReplicationConfiguration", false),
	/**
	 * Maps the <code>s3:PutStorageLensConfiguration</code> bucket policy action.
	 */
	S3_PUT_STORAGE_LENS_CONFIGURATION("s3:PutStorageLensConfiguration", false),
	/**
	 * Maps the <code>s3:PutStorageLensConfigurationTagging</code> bucket policy action.
	 */
	S3_PUT_STORAGE_LENS_CONFIGURATION_TAGGING("s3:PutStorageLensConfigurationTagging", false),
	/**
	 * Maps the <code>s3:ReplicateDelete</code> bucket policy action.
	 */
	S3_REPLICATE_DELETE("s3:ReplicateDelete", false),
	/**
	 * Maps the <code>s3:ReplicateObject</code> bucket policy action.
	 */
	S3_REPLICATE_OBJECT("s3:ReplicateObject", false),
	/**
	 * Maps the <code>s3:ReplicateTags</code> bucket policy action.
	 */
	S3_REPLICATE_TAGS("s3:ReplicateTags", false),
	/**
	 * Maps the <code>s3:RestoreObject</code> bucket policy action.
	 */
	S3_RESTORE_OBJECT("s3:RestoreObject", false),
	/**
	 * Maps the <code>s3:SubmitMultiRegionAccessPointRoutes</code> bucket policy action.
	 */
	S3_SUBMIT_MULTI_REGION_ACCESS_POINT_ROUTES("s3:SubmitMultiRegionAccessPointRoutes", false),
	/**
	 * Maps the <code>s3:TagResource</code> bucket policy action.
	 */
	S3_TAG_RESOURCE("s3:TagResource", false),
	/**
	 * Maps the <code>s3:UntagResource</code> bucket policy action.
	 */
	S3_UNTAG_RESOURCE("s3:UntagResource", false),
	/**
	 * Maps the <code>s3:UpdateAccessGrantsLocation</code> bucket policy action.
	 */
	S3_UPDATE_ACCESS_GRANTS_LOCATION("s3:UpdateAccessGrantsLocation", false),
	/**
	 * Maps the <code>s3:UpdateBucketMetadataInventoryTableConfiguration</code> bucket policy
	 * action.
	 */
	S3_UPDATE_BUCKET_METADATA_INVENTORY_TABLE_CONFIGURATION(
		"s3:UpdateBucketMetadataInventoryTableConfiguration",
		false),
	/**
	 * Maps the <code>s3:UpdateBucketMetadataJournalTableConfiguration</code> bucket policy action.
	 */
	S3_UPDATE_BUCKET_METADATA_JOURNAL_TABLE_CONFIGURATION(
		"s3:UpdateBucketMetadataJournalTableConfiguration",
		false),
	/**
	 * Maps the <code>s3:UpdateJobPriority</code> bucket policy action.
	 */
	S3_UPDATE_JOB_PRIORITY("s3:UpdateJobPriority", false),
	/**
	 * Maps the <code>s3:UpdateJobStatus</code> bucket policy action.
	 */
	S3_UPDATE_JOB_STATUS("s3:UpdateJobStatus", false),
	/**
	 * Maps the <code>s3:UpdateStorageLensGroup</code> bucket policy action.
	 */
	S3_UPDATE_STORAGE_LENS_GROUP("s3:UpdateStorageLensGroup", false);

	private final String tag;

	private final boolean isReadOnly;

	private BucketAction(String tag, boolean readOnly)
	{
		this.tag = tag;
		isReadOnly = readOnly;
	}

	/**
	 * The wrapper method of the {@link #tag} property.
	 *
	 * @return the value of the property
	 */
	public String tag()
	{
		return tag;
	}

	/**
	 * The wrapper method of the {@link #isReadOnly} property.
	 *
	 * @return the value of the property
	 */
	public boolean isReadOnly()
	{
		return isReadOnly;
	}

	/**
	 * Checks whether this action matches the given action string, supporting both exact matches and
	 * wildcard patterns ending with <code>*</code>.
	 * <p>
	 * If <code>action</code> ends with <code>*</code>, it is treated as a prefix pattern and this
	 * method returns <code>true</code> if the action's {@link #tag()} starts with the given prefix.
	 * Otherwise, an exact match against {@link #tag()} is performed.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li><code>s3:GetObject</code> matches only {@link #S3_GET_OBJECT}</li>
	 * <li><code>s3:Get*</code> matches all actions whose tag starts with <code>s3:Get</code></li>
	 * </ul>
	 *
	 * @param action
	 *            the action string to match against, optionally ending with <code>*</code>;
	 *            <code>null</code> always returns <code>false</code>
	 * @return <code>true</code> if this action matches the given string or pattern,
	 *         <code>false</code> otherwise
	 */
	public boolean matches(String action)
	{
		var wildcard = "*";
		// SONAR: partially covered by tests (3 of 4 conditions). This is a false positive because
		// if the first condition is true, Java does not evaluate the second, thus real conditions
		// are not 2 ^ 2 but 2 ^ 2 - 1
		var isPattern = action != null && action.endsWith(wildcard);
		var prefix = isPattern ? action.substring(0, action.indexOf(wildcard)) : action;
		var targetAction = tag();
		// SONAR: partially covered by tests (5 of 6 conditions). This is a false positive if the
		// first condition is true, Java does not evaluate the second, thus real conditions are not
		// 2 ^ 3 but 2 ^ 3 - 2 ^ 2 - 1
		return action != null && isPattern && targetAction.startsWith(prefix)
			|| Objects.equals(targetAction, action);
	}

	/**
	 * Filters of all items representing read-only actions, i.e. items whose {@link #isReadOnly()}
	 * method returns <code>true</code>.
	 *
	 * @return the list in the form of a stream
	 */
	public static Stream<BucketAction> readOnlyActions()
	{
		return Stream.of(values()).filter(BucketAction::isReadOnly);
	}

	/**
	 * Filters of all items representing writable actions, i.e. items whose {@link #isReadOnly()}
	 * method returns <code>false</code>.
	 *
	 * @return the list in the form of a stream
	 */
	public static Stream<BucketAction> writeActions()
	{
		return Stream.of(values()).filter(Predicate.not(BucketAction::isReadOnly));
	}

	/**
	 * Filters of all items matching the given action, i.e. items whose {@link #matches(String)}
	 * method returns <code>true</code>.
	 *
	 * @param action
	 *            the native S3 action
	 * @return the list in the form of a stream
	 */
	public static Stream<BucketAction> matchingActions(String action)
	{
		return Stream.of(BucketAction.values()).filter(item -> item.matches(action));
	}
}
