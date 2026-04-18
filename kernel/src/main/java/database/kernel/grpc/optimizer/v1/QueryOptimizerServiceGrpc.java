package database.kernel.grpc.optimizer.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.2)",
    comments = "Source: query_optimizer.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class QueryOptimizerServiceGrpc {

  private QueryOptimizerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "minipostgres.optimizer.v1.QueryOptimizerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.PredictRequest,
      database.kernel.grpc.optimizer.v1.PredictResponse> getPredictMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Predict",
      requestType = database.kernel.grpc.optimizer.v1.PredictRequest.class,
      responseType = database.kernel.grpc.optimizer.v1.PredictResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.PredictRequest,
      database.kernel.grpc.optimizer.v1.PredictResponse> getPredictMethod() {
    io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.PredictRequest, database.kernel.grpc.optimizer.v1.PredictResponse> getPredictMethod;
    if ((getPredictMethod = QueryOptimizerServiceGrpc.getPredictMethod) == null) {
      synchronized (QueryOptimizerServiceGrpc.class) {
        if ((getPredictMethod = QueryOptimizerServiceGrpc.getPredictMethod) == null) {
          QueryOptimizerServiceGrpc.getPredictMethod = getPredictMethod =
              io.grpc.MethodDescriptor.<database.kernel.grpc.optimizer.v1.PredictRequest, database.kernel.grpc.optimizer.v1.PredictResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Predict"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.optimizer.v1.PredictRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.optimizer.v1.PredictResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryOptimizerServiceMethodDescriptorSupplier("Predict"))
              .build();
        }
      }
    }
    return getPredictMethod;
  }

  private static volatile io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.RewardRequest,
      database.kernel.grpc.optimizer.v1.RewardResponse> getReportRewardMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportReward",
      requestType = database.kernel.grpc.optimizer.v1.RewardRequest.class,
      responseType = database.kernel.grpc.optimizer.v1.RewardResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.RewardRequest,
      database.kernel.grpc.optimizer.v1.RewardResponse> getReportRewardMethod() {
    io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.RewardRequest, database.kernel.grpc.optimizer.v1.RewardResponse> getReportRewardMethod;
    if ((getReportRewardMethod = QueryOptimizerServiceGrpc.getReportRewardMethod) == null) {
      synchronized (QueryOptimizerServiceGrpc.class) {
        if ((getReportRewardMethod = QueryOptimizerServiceGrpc.getReportRewardMethod) == null) {
          QueryOptimizerServiceGrpc.getReportRewardMethod = getReportRewardMethod =
              io.grpc.MethodDescriptor.<database.kernel.grpc.optimizer.v1.RewardRequest, database.kernel.grpc.optimizer.v1.RewardResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportReward"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.optimizer.v1.RewardRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.optimizer.v1.RewardResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryOptimizerServiceMethodDescriptorSupplier("ReportReward"))
              .build();
        }
      }
    }
    return getReportRewardMethod;
  }

  private static volatile io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.HealthCheckRequest,
      database.kernel.grpc.optimizer.v1.HealthCheckResponse> getHealthCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HealthCheck",
      requestType = database.kernel.grpc.optimizer.v1.HealthCheckRequest.class,
      responseType = database.kernel.grpc.optimizer.v1.HealthCheckResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.HealthCheckRequest,
      database.kernel.grpc.optimizer.v1.HealthCheckResponse> getHealthCheckMethod() {
    io.grpc.MethodDescriptor<database.kernel.grpc.optimizer.v1.HealthCheckRequest, database.kernel.grpc.optimizer.v1.HealthCheckResponse> getHealthCheckMethod;
    if ((getHealthCheckMethod = QueryOptimizerServiceGrpc.getHealthCheckMethod) == null) {
      synchronized (QueryOptimizerServiceGrpc.class) {
        if ((getHealthCheckMethod = QueryOptimizerServiceGrpc.getHealthCheckMethod) == null) {
          QueryOptimizerServiceGrpc.getHealthCheckMethod = getHealthCheckMethod =
              io.grpc.MethodDescriptor.<database.kernel.grpc.optimizer.v1.HealthCheckRequest, database.kernel.grpc.optimizer.v1.HealthCheckResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HealthCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.optimizer.v1.HealthCheckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.optimizer.v1.HealthCheckResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryOptimizerServiceMethodDescriptorSupplier("HealthCheck"))
              .build();
        }
      }
    }
    return getHealthCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QueryOptimizerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryOptimizerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryOptimizerServiceStub>() {
        @java.lang.Override
        public QueryOptimizerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryOptimizerServiceStub(channel, callOptions);
        }
      };
    return QueryOptimizerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QueryOptimizerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryOptimizerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryOptimizerServiceBlockingStub>() {
        @java.lang.Override
        public QueryOptimizerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryOptimizerServiceBlockingStub(channel, callOptions);
        }
      };
    return QueryOptimizerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QueryOptimizerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryOptimizerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryOptimizerServiceFutureStub>() {
        @java.lang.Override
        public QueryOptimizerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryOptimizerServiceFutureStub(channel, callOptions);
        }
      };
    return QueryOptimizerServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Predict the best scan strategy for a query state.
     * Expected latency: &lt; 5ms (in-memory Q-table lookup)
     * </pre>
     */
    default void predict(database.kernel.grpc.optimizer.v1.PredictRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.PredictResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPredictMethod(), responseObserver);
    }

    /**
     * <pre>
     * Report execution reward for RL learning.
     * Idempotent via idempotency_key — safe to retry.
     * </pre>
     */
    default void reportReward(database.kernel.grpc.optimizer.v1.RewardRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.RewardResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportRewardMethod(), responseObserver);
    }

    /**
     * <pre>
     * Health check for the optimizer service.
     * </pre>
     */
    default void healthCheck(database.kernel.grpc.optimizer.v1.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.HealthCheckResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHealthCheckMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service QueryOptimizerService.
   */
  public static abstract class QueryOptimizerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return QueryOptimizerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service QueryOptimizerService.
   */
  public static final class QueryOptimizerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<QueryOptimizerServiceStub> {
    private QueryOptimizerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryOptimizerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryOptimizerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Predict the best scan strategy for a query state.
     * Expected latency: &lt; 5ms (in-memory Q-table lookup)
     * </pre>
     */
    public void predict(database.kernel.grpc.optimizer.v1.PredictRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.PredictResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPredictMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Report execution reward for RL learning.
     * Idempotent via idempotency_key — safe to retry.
     * </pre>
     */
    public void reportReward(database.kernel.grpc.optimizer.v1.RewardRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.RewardResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportRewardMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Health check for the optimizer service.
     * </pre>
     */
    public void healthCheck(database.kernel.grpc.optimizer.v1.HealthCheckRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.HealthCheckResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service QueryOptimizerService.
   */
  public static final class QueryOptimizerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<QueryOptimizerServiceBlockingStub> {
    private QueryOptimizerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryOptimizerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryOptimizerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Predict the best scan strategy for a query state.
     * Expected latency: &lt; 5ms (in-memory Q-table lookup)
     * </pre>
     */
    public database.kernel.grpc.optimizer.v1.PredictResponse predict(database.kernel.grpc.optimizer.v1.PredictRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPredictMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Report execution reward for RL learning.
     * Idempotent via idempotency_key — safe to retry.
     * </pre>
     */
    public database.kernel.grpc.optimizer.v1.RewardResponse reportReward(database.kernel.grpc.optimizer.v1.RewardRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportRewardMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Health check for the optimizer service.
     * </pre>
     */
    public database.kernel.grpc.optimizer.v1.HealthCheckResponse healthCheck(database.kernel.grpc.optimizer.v1.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHealthCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service QueryOptimizerService.
   */
  public static final class QueryOptimizerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<QueryOptimizerServiceFutureStub> {
    private QueryOptimizerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryOptimizerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryOptimizerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Predict the best scan strategy for a query state.
     * Expected latency: &lt; 5ms (in-memory Q-table lookup)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<database.kernel.grpc.optimizer.v1.PredictResponse> predict(
        database.kernel.grpc.optimizer.v1.PredictRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPredictMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Report execution reward for RL learning.
     * Idempotent via idempotency_key — safe to retry.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<database.kernel.grpc.optimizer.v1.RewardResponse> reportReward(
        database.kernel.grpc.optimizer.v1.RewardRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportRewardMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Health check for the optimizer service.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<database.kernel.grpc.optimizer.v1.HealthCheckResponse> healthCheck(
        database.kernel.grpc.optimizer.v1.HealthCheckRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHealthCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PREDICT = 0;
  private static final int METHODID_REPORT_REWARD = 1;
  private static final int METHODID_HEALTH_CHECK = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PREDICT:
          serviceImpl.predict((database.kernel.grpc.optimizer.v1.PredictRequest) request,
              (io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.PredictResponse>) responseObserver);
          break;
        case METHODID_REPORT_REWARD:
          serviceImpl.reportReward((database.kernel.grpc.optimizer.v1.RewardRequest) request,
              (io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.RewardResponse>) responseObserver);
          break;
        case METHODID_HEALTH_CHECK:
          serviceImpl.healthCheck((database.kernel.grpc.optimizer.v1.HealthCheckRequest) request,
              (io.grpc.stub.StreamObserver<database.kernel.grpc.optimizer.v1.HealthCheckResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getPredictMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              database.kernel.grpc.optimizer.v1.PredictRequest,
              database.kernel.grpc.optimizer.v1.PredictResponse>(
                service, METHODID_PREDICT)))
        .addMethod(
          getReportRewardMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              database.kernel.grpc.optimizer.v1.RewardRequest,
              database.kernel.grpc.optimizer.v1.RewardResponse>(
                service, METHODID_REPORT_REWARD)))
        .addMethod(
          getHealthCheckMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              database.kernel.grpc.optimizer.v1.HealthCheckRequest,
              database.kernel.grpc.optimizer.v1.HealthCheckResponse>(
                service, METHODID_HEALTH_CHECK)))
        .build();
  }

  private static abstract class QueryOptimizerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QueryOptimizerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return database.kernel.grpc.optimizer.v1.QueryOptimizerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QueryOptimizerService");
    }
  }

  private static final class QueryOptimizerServiceFileDescriptorSupplier
      extends QueryOptimizerServiceBaseDescriptorSupplier {
    QueryOptimizerServiceFileDescriptorSupplier() {}
  }

  private static final class QueryOptimizerServiceMethodDescriptorSupplier
      extends QueryOptimizerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    QueryOptimizerServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (QueryOptimizerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QueryOptimizerServiceFileDescriptorSupplier())
              .addMethod(getPredictMethod())
              .addMethod(getReportRewardMethod())
              .addMethod(getHealthCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
