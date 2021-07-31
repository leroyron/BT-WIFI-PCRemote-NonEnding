using System;
using System.IO;
using System.Threading;
using WindowsInput.Native;
using static TrayIconBluetoothController.MainForm;

namespace TrayIconBluetoothController
{
    public class VirtualKeyboard
    {
        private const byte EMPTY = 0;

        private static WindowsInput.InputSimulator mInput = new WindowsInput.InputSimulator();

        private readonly MainForm form;

        public VirtualKeyboard(MainForm form1)
        {
            this.form = form1;
        }


        public static void sendKey(byte keyDown1 = 0, byte keyDown2 = 0, byte keyPress = 0, int mms = 0)
        {
            VirtualKeyCode key1 = 0, key2 = 0, press = 0;
            //Thread.Sleep(mms);
            Console.WriteLine("Received bytes: {0}, {1}, {2}", keyDown1, keyDown2, keyPress);
            if (keyDown1 == 17 && keyDown2 == 18 &&  keyPress == 121)
            {
                System.Diagnostics.Process.Start(@"C:\WINDOWS\system32\rundll32.exe", "user32.dll,LockWorkStation");
                /*keyDown1 = 91;
                keyPress = 88;
                key1 = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyDown1.ToString());
                press = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyPress.ToString());
                mInput.Keyboard.KeyDown(key1);
                mInput.Keyboard.KeyPress(press);
                mInput.Keyboard.KeyUp(key1);

                Thread.Sleep(500);
                keyPress = 85;
                press = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyPress.ToString());
                mInput.Keyboard.KeyPress(press);

                Thread.Sleep(500);
                keyPress = 83;
                press = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyPress.ToString());
                mInput.Keyboard.KeyPress(press);*/
                return;
            }

            if (keyDown1 != EMPTY)
            {
                key1 = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyDown1.ToString());
                mInput.Keyboard.KeyDown(key1);
            }

            if (keyDown2 != EMPTY)
            {
                key2 = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyDown2.ToString());
                mInput.Keyboard.KeyDown(key2);
            }

            if (keyPress != EMPTY)
            {
                press = (VirtualKeyCode)Enum.Parse(typeof(VirtualKeyCode), keyPress.ToString());
                mInput.Keyboard.KeyPress(press);
            }

            if (keyDown1 != EMPTY)
            {
                mInput.Keyboard.KeyUp(key1);

            }

            if (keyDown2 != EMPTY)
            {
                mInput.Keyboard.KeyUp(key2);
            }

            return;
        }

        public static void readWhileOpen(Stream peerStream) {
            try {
                while (true) {
                    byte[] buf = new byte[3];
                    int readLen = peerStream.Read(buf, 0, 3);
                    if (readLen > 1)
                        sendKey(buf[0], buf[1], buf[2]);
                    else
                        return;
                }
            } catch {
                Console.WriteLine("Stream closed");
            }
        }
    }
}
