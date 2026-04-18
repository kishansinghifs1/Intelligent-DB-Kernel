package database.kernel.grpc.query.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.2)",
    comments = "Source: query_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class QueryServiceGrpc {

  private QueryServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "minipostgres.query.v1.QueryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.ExecuteRequest,
      database.kernel.grpc.query.v1.ExecuteResponse> getExecuteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Execute",
      requestType = database.kernel.grpc.query.v1.ExecuteRequest.class,
      responseType = database.kernel.grpc.query.v1.ExecuteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.ExecuteRequest,
      database.kernel.grpc.query.v1.ExecuteResponse> getExecuteMethod() {
    io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.ExecuteRequest, database.kernel.grpc.query.v1.ExecuteResponse> getExecuteMethod;
    if ((getExecuteMethod = QueryServiceGrpc.getExecuteMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getExecuteMethod = QueryServiceGrpc.getExecuteMethod) == null) {
          QueryServiceGrpc.getExecuteMethod = getExecuteMethod =
              io.grpc.MethodDescriptor.<database.kernel.grpc.query.v1.ExecuteRequest, database.kernel.grpc.query.v1.ExecuteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Execute"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.query.v1.ExecuteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.query.v1.ExecuteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("Execute"))
              .build();
        }
      }
    }
    return getExecuteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.StateRequest,
      database.kernel.grpc.query.v1.StateResponse> getGetStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetState",
      requestType = database.kernel.grpc.query.v1.StateRequest.class,
      responseType = database.kernel.grpc.query.v1.StateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.StateRequest,
      database.kernel.grpc.query.v1.StateResponse> getGetStateMethod() {
    io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.StateRequest, database.kernel.grpc.query.v1.StateResponse> getGetStateMethod;
    if ((getGetStateMethod = QueryServiceGrpc.getGetStateMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getGetStateMethod = QueryServiceGrpc.getGetStateMethod) == null) {
          QueryServiceGrpc.getGetStateMethod = getGetStateMethod =
              io.grpc.MethodDescriptor.<database.kernel.grpc.query.v1.StateRequest, database.kernel.grpc.query.v1.StateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.query.v1.StateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.query.v1.StateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("GetState"))
              .build();
        }
      }
    }
    return getGetStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.ExecuteRequest,
      database.kernel.grpc.query.v1.QueryResultRow> getExecuteStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteStream",
      requestType = database.kernel.grpc.query.v1.ExecuteRequest.class,
      responseType = database.kernel.grpc.query.v1.QueryResultRow.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.ExecuteRequest,
      database.kernel.grpc.query.v1.QueryResultRow> getExecuteStreamMethod() {
    io.grpc.MethodDescriptor<database.kernel.grpc.query.v1.ExecuteRequest, database.kernel.grpc.query.v1.QueryResultRow> getExecuteStreamMethod;
    if ((getExecuteStreamMethod = QueryServiceGrpc.getExecuteStreamMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getExecuteStreamMethod = QueryServiceGrpc.getExecuteStreamMethod) == null) {
          QueryServiceGrpc.getExecuteStreamMethod = getExecuteStreamMethod =
              io.grpc.MethodDescriptor.<database.kernel.grpc.query.v1.ExecuteRequest, database.kernel.grpc.query.v1.QueryResultRow>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.query.v1.ExecuteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  database.kernel.grpc.query.v1.QueryResultRow.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("ExecuteStream"))
              .build();
        }
      }
    }
    return getExecuteStreamMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QueryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryServiceStub>() {
        @java.lang.Override
        public QueryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryServiceStub(channel, callOptions);
        }
      };
    return QueryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QueryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryServiceBlockingStub>() {
        @java.lang.Override
        public QueryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryServiceBlockingStub(channel, callOptions);
        }
      };
    return QueryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QueryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryServiceFutureStub>() {
        @java.lang.Override
        public QueryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryServiceFutureStub(channel, callOptions);
        }
      };
    return QueryServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Execute one or more SQL commands.
     * </pre>
     */
    default void execute(database.kernel.grpc.query.v1.ExecuteRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.ExecuteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExecuteMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get current database state (tables, indexes, data files).
     * </pre>
     */
    default void getState(database.kernel.grpc.query.v1.StateRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.StateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetStateMethod(), responseObserver);
    }

    /**
     * <pre>
     * Stream query results for large result sets.
     * </pre>
     */
    default void executeStream(database.kernel.grpc.query.v1.ExecuteRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.QueryResultRow> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getExecuteStreamMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service QueryService.
   */
  public static abstract class QueryServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return QueryServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service QueryService.
   */
  public static final class QueryServiceStub
      extends io.grpc.stub.AbstractAsyncStub<QueryServiceStub> {
    private QueryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Execute one or more SQL commands.
     * </pre>
     */
    public void execute(database.kernel.grpc.query.v1.ExecuteRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.ExecuteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getExecuteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get current database state (tables, indexes, data files).
     * </pre>
     */
    public void getState(database.kernel.grpc.query.v1.StateRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.StateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetStateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Stream query results for large result sets.
     * </pre>
     */
    public void executeStream(database.kernel.grpc.query.v1.ExecuteRequest request,
        io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.QueryResultRow> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getExecuteStreamMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service QueryService.
   */
  public static final class QueryServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<QueryServiceBlockingStub> {
    private QueryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Execute one or more SQL commands.
     * </pre>
     */
    public database.kernel.grpc.query.v1.ExecuteResponse execute(database.kernel.grpc.query.v1.ExecuteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getExecuteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get current database state (tables, indexes, data files).
     * </pre>
     */
    public database.kernel.grpc.query.v1.StateResponse getState(database.kernel.grpc.query.v1.StateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetStateMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Stream query results for large result sets.
     * </pre>
     */
    public java.util.Iterator<database.kernel.grpc.query.v1.QueryResultRow> executeStream(
        database.kernel.grpc.query.v1.ExecuteRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getExecuteStreamMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service QueryService.
   */
  public static final class QueryServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<QueryServiceFutureStub> {
    private QueryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Execute one or more SQL commands.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<database.kernel.grpc.query.v1.ExecuteResponse> execute(
        database.kernel.grpc.query.v1.ExecuteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getExecuteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get current database state (tables, indexes, data files).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<database.kernel.grpc.query.v1.StateResponse> getState(
        database.kernel.grpc.query.v1.StateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetStateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE = 0;
  private static final int METHODID_GET_STATE = 1;
  private static final int METHODID_EXECUTE_STREAM = 2;

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
        case METHODID_EXECUTE:
          serviceImpl.execute((database.kernel.grpc.query.v1.ExecuteRequest) request,
              (io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.ExecuteResponse>) responseObserver);
          break;
        case METHODID_GET_STATE:
          serviceImpl.getState((database.kernel.grpc.query.v1.StateRequest) request,
              (io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.StateResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_STREAM:
          serviceImpl.executeStream((database.kernel.grpc.query.v1.ExecuteRequest) request,
              (io.grpc.stub.StreamObserver<database.kernel.grpc.query.v1.QueryResultRow>) responseObserver);
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
          getExecuteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              database.kernel.grpc.query.v1.ExecuteRequest,
              database.kernel.grpc.query.v1.ExecuteResponse>(
                service, METHODID_EXECUTE)))
        .addMethod(
          getGetStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              database.kernel.grpc.query.v1.StateRequest,
              database.kernel.grpc.query.v1.StateResponse>(
                service, METHODID_GET_STATE)))
        .addMethod(
          getExecuteStreamMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              database.kernel.grpc.query.v1.ExecuteRequest,
              database.kernel.grpc.query.v1.QueryResultRow>(
                service, METHODID_EXECUTE_STREAM)))
        .build();
  }

  private static abstract class QueryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QueryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return database.kernel.grpc.query.v1.QueryServiceProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QueryService");
    }
  }

  private static final class QueryServiceFileDescriptorSupplier
      extends QueryServiceBaseDescriptorSupplier {
    QueryServiceFileDescriptorSupplier() {}
  }

  private static final class QueryServiceMethodDescriptorSupplier
      extends QueryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    QueryServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (QueryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QueryServiceFileDescriptorSupplier())
              .addMethod(getExecuteMethod())
              .addMethod(getGetStateMethod())
              .addMethod(getExecuteStreamMethod())
              .build();
        }
      }
    }
    return result;
  }
}
