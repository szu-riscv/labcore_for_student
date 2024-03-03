
module tb_top(
    input  wire rxd,
    output wire txd
    // input  wire reset
);

    // 
    // import difftest interface
    //
    import "DPI-C" function void set_bin_file(string bin);
    import "DPI-C" function void simv_init();
    import "DPI-C" function int simv_step();
    
    reg           clock;
    reg           reset;

    string bin_file;
    initial begin
        clock = 0;
        reset = 1;

        // 
        // workload: bin file
        // 
        if ($test$plusargs("workload")) begin
            $value$plusargs("workload=%s", bin_file);
            set_bin_file(bin_file);
        end
    end

    always #10 clock = ~clock;

    reg [63:0] cycles;
    initial cycles = 0;
    always@(posedge clock) begin
        cycles <= cycles + 1;
    end

    wire       startWork;
    wire       io_uartTxData_valid;
    wire [7:0] io_uartTxData_bits;
    
    Top top(
        .io_clock(clock),
        .io_rst_n(~reset),
        .io_uart_txd(txd),
        .io_uart_rxd(rxd),
        .io_startWork(startWork),
        .io_uartTxData_valid(io_uartTxData_valid),
        .io_uartTxData_bits(io_uartTxData_bits)
    );

    // 
    // difftest step
    // 
    reg has_init;
    always @(posedge clock) begin
        if (reset) begin
            has_init <= 1'b0;
        end
        else if (!has_init) begin
            $display("[%d] start simv_init()", cycles);
            simv_init();
            has_init <= 1'b1;
        end

        // check errors
        if (!reset && has_init && startWork) begin
            // $display("[%d] start simv_step()", cycles);
            if (simv_step()) begin
                $finish();
            end
        end
    end

    // 
    // uart output
    // 
    always @(posedge clock) begin
        if (!reset && io_uartTxData_valid) begin
            $fwrite(32'h8000_0001, "%c", io_uartTxData_bits);
            $fflush();
            $finish();
        end
    end      

    initial begin
        // 
        // dump VCD
        // 
        // if ($test$plusargs("dump_enable")) begin
        //     $dumpfile("dump.vcd");
        //     $dumpvars(0, tb_top);
        // end

        // 
        // dump FSDB
        // 
        if ($test$plusargs("dump_enable")) begin
            $fsdbDumpfile("dump.fsdb");
            $fsdbDumpvars(0, tb_top);
        end
    end



endmodule