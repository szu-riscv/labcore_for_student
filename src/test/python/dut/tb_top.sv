
module tb_top(
    input  wire rxd,
    output wire txd
    // input  wire reset
);

    reg           clock;
    reg           reset;

    initial begin
        clock = 0;
        reset = 0;
    end

    always #10 clock = ~clock;

    reg [63:0] cycles;
    initial cycles = 0;
    always@(posedge clock) begin
        cycles <= cycles + 1;
    end


    Top top(
        .clock(clock),
        .reset(reset),
        .io_uart_txd(txd),
        .io_uart_rxd(rxd)
    );

    initial begin
        if ($test$plusargs("dump_enable")) begin
            $dumpfile("dump.vcd");
            $dumpvars(0, tb_top);
        end
    end



endmodule