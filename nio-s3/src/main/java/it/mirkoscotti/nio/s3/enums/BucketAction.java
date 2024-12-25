/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.enums;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public enum BucketAction
{

	S3_DESCRIBE_JOB("s3:DescribeJob", true),
	S3_DESCRIBE_MULTI_REGION_ACCESS_POINT_OPERATION(
		"s3:DescribeMultiRegionAccessPointOperation",
		true),
	S3_GET_ACCESS_POINT("s3:GetAccessPoint", true),
	S3_GET_ACCESS_POINT_CONFIGURATION_FOR_OBJECT_LAMBDA(
		"s3:GetAccessPointConfigurationForObjectLambda",
		true),
	S3_GET_ACCESS_POINT_FOR_OBJECT_LAMBDA("s3:GetAccessPointForObjectLambda", true),
	S3_GET_ACCESS_POINT_POLICY("s3:GetAccessPointPolicy", true),
	S3_GET_ACCESS_POINT_POLICY_FOR_OBJECT_LAMBDA("s3:GetAccessPointPolicyForObjectLambda", true),
	S3_GET_ACCESS_POINT_POLICY_STATUS("s3:GetAccessPointPolicyStatus", true),
	S3_GET_ACCESS_POINT_POLICY_STATUS_FOR_OBJECT_LAMBDA(
		"s3:GetAccessPointPolicyStatusForObjectLambda",
		true),
	S3_GET_ACCELERATE_CONFIGURATION("s3:GetAccelerateConfiguration", true),
	S3_GET_ANALYTICS_CONFIGURATION("s3:GetAnalyticsConfiguration", true),
	S3_GET_BUCKET_ACL("s3:GetBucketAcl", true),
	S3_GET_BUCKET_CORS("s3:GetBucketCORS", true),
	S3_GET_BUCKET_LOCATION("s3:GetBucketLocation", true),
	S3_GET_BUCKET_LOGGING("s3:GetBucketLogging", true),
	S3_GET_BUCKET_NOTIFICATION("s3:GetBucketNotification", true),
	S3_GET_BUCKET_OBJECT_LOCK_CONFIGURATION("s3:GetBucketObjectLockConfiguration", true),
	S3_GET_BUCKET_POLICY("s3:GetBucketPolicy", true),
	S3_GET_BUCKET_POLICY_STATUS("s3:GetBucketPolicyStatus", true),
	S3_GET_BUCKET_PUBLIC_ACCESS_BLOCK("s3:GetBucketPublicAccessBlock", true),
	S3_GET_BUCKET_REQUEST_PAYMENT("s3:GetBucketRequestPayment", true),
	S3_GET_BUCKET_TAGGING("s3:GetBucketTagging", true),
	S3_GET_BUCKET_VERSIONING("s3:GetBucketVersioning", true),
	S3_GET_BUCKET_WEBSITE("s3:GetBucketWebsite", true),
	S3_GET_ENCRYPTION_CONFIGURATION("s3:GetEncryptionConfiguration", true),
	S3_GET_INTELLIGENT_TIERING_CONFIGURATION("s3:GetIntelligentTieringConfiguration", true),
	S3_GET_INVENTORY_CONFIGURATION("s3:GetInventoryConfiguration", true),
	S3_GET_JOB_TAGGING("s3:GetJobTagging", true),
	S3_GET_LIFECYCLE_CONFIGURATION("s3:GetLifecycleConfiguration", true),
	S3_GET_METRICS_CONFIGURATION("s3:GetMetricsConfiguration", true),
	S3_GET_MULTI_REGION_ACCESS_POINT_POLICY("s3:GetMultiRegionAccessPointPolicy", true),
	S3_GET_MULTI_REGION_ACCESS_POINT_POLICY_STATUS(
		"s3:GetMultiRegionAccessPointPolicyStatus",
		true),
	S3_GET_MULTI_REGION_ACCESS_POINT_ROUTES("s3:GetMultiRegionAccessPointRoutes", true),
	S3_GET_OBJECT("s3:GetObject", true),
	S3_GET_OBJECT_ACL("s3:GetObjectAcl", true),
	S3_GET_OBJECT_ATTRIBUTES("s3:GetObjectAttributes", true),
	S3_GET_OBJECT_LEGAL_HOLD("s3:GetObjectLegalHold", true),
	S3_GET_OBJECT_RETENTION("s3:GetObjectRetention", true),
	S3_GET_OBJECT_TAGGING("s3:GetObjectTagging", true),
	S3_GET_OBJECT_TORRENT("s3:GetObjectTorrent", true),
	S3_GET_OBJECT_VERSION("s3:GetObjectVersion", true),
	S3_GET_OBJECT_VERSION_ACL("s3:GetObjectVersionAcl", true),
	S3_GET_OBJECT_VERSION_ATTRIBUTES("s3:GetObjectVersionAttributes", true),
	S3_GET_OBJECT_VERSION_FOR_REPLICATION("s3:GetObjectVersionForReplication", true),
	S3_GET_OBJECT_VERSION_TAGGING("s3:GetObjectVersionTagging", true),
	S3_GET_OBJECT_VERSION_TORRENT("s3:GetObjectVersionTorrent", true),
	S3_GET_REPLICATION_CONFIGURATION("s3:GetReplicationConfiguration", true),
	S3_GET_STORAGE_CLASS_ANALYSIS("s3:GetStorageClassAnalysis", true),
	S3_GET_STORAGE_LENS_CONFIGURATION("s3:GetStorageLensConfiguration", true),
	S3_GET_STORAGE_LENS_CONFIGURATION_TAGGING("s3:GetStorageLensConfigurationTagging", true),
	S3_GET_STORAGE_LENS_DASHBOARD("s3:GetStorageLensDashboard", true),
	S3_HEAD_BUCKET("s3:HeadBucket", true),
	S3_HEAD_OBJECT("s3:HeadObject", true),
	S3_LIST_ACCESS_POINTS("s3:ListAccessPoints", true),
	S3_LIST_ACCESS_POINTS_FOR_OBJECT_LAMBDA("s3:ListAccessPointsForObjectLambda", true),
	S3_LIST_BUCKET("s3:ListBucket", true),
	S3_LIST_BUCKET_MULTIPART_UPLOADS("s3:ListBucketMultipartUploads", true),
	S3_LIST_BUCKET_VERSIONS("s3:ListBucketVersions", true),
	S3_LIST_JOBS("s3:ListJobs", true),
	S3_LIST_MULTI_REGION_ACCESS_POINTS("s3:ListMultiRegionAccessPoints", true),
	S3_LIST_MULTIPART_UPLOAD_PARTS("s3:ListMultipartUploadParts", true),
	S3_LIST_STORAGE_CLASS_ANALYSIS("s3:ListStorageClassAnalysis", true),
	S3_LIST_STORAGE_LENS_CONFIGURATIONS("s3:ListStorageLensConfigurations", true),
	S3_ABORT_MULTIPART_UPLOAD("s3:AbortMultipartUpload", false),
	S3_CREATE_BUCKET("s3:CreateBucket", false),
	S3_DELETE_BUCKET("s3:DeleteBucket", false),
	S3_DELETE_BUCKET_POLICY("s3:DeleteBucketPolicy", false),
	S3_DELETE_BUCKET_WEBSITE("s3:DeleteBucketWebsite", false),
	S3_DELETE_OBJECT("s3:DeleteObject", false),
	S3_DELETE_OBJECT_TAGGING("s3:DeleteObjectTagging", false),
	S3_DELETE_OBJECT_VERSION("s3:DeleteObjectVersion", false),
	S3_DELETE_OBJECT_VERSION_TAGGING("s3:DeleteObjectVersionTagging", false),
	S3_PUT_ACCESS_POINT_POLICY("s3:PutAccessPointPolicy", false),
	S3_PUT_BUCKET_ACL("s3:PutBucketAcl", false),
	S3_PUT_BUCKET_CORS("s3:PutBucketCORS", false),
	S3_PUT_BUCKET_LOGGING("s3:PutBucketLogging", false),
	S3_PUT_BUCKET_NOTIFICATION("s3:PutBucketNotification", false),
	S3_PUT_BUCKET_OBJECT_LOCK_CONFIGURATION("s3:PutBucketObjectLockConfiguration", false),
	S3_PUT_BUCKET_POLICY("s3:PutBucketPolicy", false),
	S3_PUT_BUCKET_REQUEST_PAYMENT("s3:PutBucketRequestPayment", false),
	S3_PUT_BUCKET_TAGGING("s3:PutBucketTagging", false),
	S3_PUT_BUCKET_VERSIONING("s3:PutBucketVersioning", false),
	S3_PUT_BUCKET_WEBSITE("s3:PutBucketWebsite", false),
	S3_PUT_ENCRYPTION_CONFIGURATION("s3:PutEncryptionConfiguration", false),
	S3_PUT_LIFECYCLE_CONFIGURATION("s3:PutLifecycleConfiguration", false),
	S3_PUT_OBJECT("s3:PutObject", false),
	S3_PUT_OBJECT_ACL("s3:PutObjectAcl", false),
	S3_PUT_OBJECT_LEGAL_HOLD("s3:PutObjectLegalHold", false),
	S3_PUT_OBJECT_RETENTION("s3:PutObjectRetention", false),
	S3_PUT_OBJECT_TAGGING("s3:PutObjectTagging", false),
	S3_PUT_OBJECT_VERSION_ACL("s3:PutObjectVersionAcl", false),
	S3_PUT_OBJECT_VERSION_TAGGING("s3:PutObjectVersionTagging", false),
	S3_PUT_REPLICATION_CONFIGURATION("s3:PutReplicationConfiguration", false),
	S3_REPLICATE_DELETE("s3:ReplicateDelete", false),
	S3_REPLICATE_OBJECT("s3:ReplicateObject", false),
	S3_RESTORE_OBJECT("s3:RestoreObject", false);

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

	public static Stream<BucketAction> readOnlyActions()
	{
		return Stream.of(values()).filter(BucketAction::isReadOnly);
	}

	public static Stream<BucketAction> writeActions()
	{
		return Stream.of(values()).filter(Predicate.not(BucketAction::isReadOnly));
	}

	public static Stream<BucketAction> matchingActions(String action)
	{
		return Stream.of(BucketAction.values()).filter(item -> item.matches(action));
	}
}
