import re
import os
import argparse

def main():
    parser = argparse.ArgumentParser(description='verilog module extractor, seperate every single module within a single verilog file')

    parser.add_argument('--file', '-f', dest="file", type=str, help='input verilog file')
    parser.add_argument('--output', '-o', dest="out_dir", type=str, help='output directory')
    parser.add_argument("--output_count_prefix", '-p', dest="prefix", action="store_true", help="enable output file prefix with current module count")

    args = parser.parse_args()
    prefix = args.prefix
    assert args.file != None
    assert args.out_dir != None
    
    file = os.path.abspath(args.file)
    out_dir = os.path.abspath(args.out_dir)
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
    
    file_path = os.path.dirname(os.path.abspath(file))
    file_name = os.path.basename(file)


    os.system('rm '+ out_dir + '/' + '*' )
    with open(file, 'rt') as f:
        find_module = False
        find_endmodule = False
        module_name = ''
        wf = None
        lf = open(file_path+f'/{file_name}.log', 'wt')
        module_count = 0
        line_num = 1
        for line in f:
            if(line.count('module') != 0 and find_module == False):
                find_module = True
                module_name = re.split(r'[;,\s(]\s*',line)[1]
                module_count = module_count + 1
                strr = '['+str(module_count)+'] find module => '+module_name + ' in line ' + str(line_num)
                print(strr)
                print(strr, file=lf)
                if prefix:
                    file_name = out_dir + '/' + str(module_count) + '_' + module_name + '.v'
                else:
                    file_name = out_dir + '/' + module_name + '.v'
                wf = open(file_name,'wt')
            
            if(line.count('endmodule') != 0):
                find_endmodule = True

            if(find_module == True):
                print(line,end='',file=wf)
                temp_line = re.split(r'[;,\s]\s*',line)
                if(temp_line.count('(') == 1 and temp_line.count(')') == 0):
                    # print(temp_line)
                    # strr = '\tfind submodule => ('+temp_line[1] +')\t('+ temp_line[2] +') in line ['+str(line_num)+']'
                    strr = '\tfind submodule => module_name:( %-*s)  inst_name:( %-*s) in line [ %-*s ]'%(20,temp_line[1],20,temp_line[2],6,str(line_num))
                    print(strr)
                    print(strr, file=lf)
                
            if(find_endmodule == True):
                find_module = False
                find_endmodule = False
                strr = module_name+' done!' + ' in line ' + str(line_num) + '\n'
                print(strr)
                print(strr,file=lf)
                try:
                    wf.close()
                except:
                    pass
            line_num = line_num + 1
        lf.close()
        # os.system('rm '+output_path+'.v')


if __name__ == '__main__': main()
