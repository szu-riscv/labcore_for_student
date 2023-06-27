
module tb_top;

    reg           clock;
    reg           reset;

    initial begin
        clock = 0;
        reset = 0;
    end

    reg [63:0] cycles;
    always@(posedge clock) begin
        cycles <= cycles + 1;
    end


    wire rxd;
    reg txd;

    initial begin
        txd = 1;
    end


    Top top(
        .clock(clock),
        .reset(reset),
        .io_uart_txd(rxd),
        .io_uart_rxd(txd)
    );

    initial begin
        if ($test$plusargs("dump_enable")) begin
            $dumpfile("dump.vcd");
            $dumpvars(0, tb_top);
        end
    end



endmodule